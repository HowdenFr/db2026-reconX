package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.ReconciliationEngine;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DlqRoutingIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private TradeEventProducer producer;

    @MockBean
    private ReconciliationEngine reconEngine;

    @Test
    void failingConsumerRoutesToDlq() {
        Mockito.doThrow(new RuntimeException("boom"))
                .when(reconEngine).scheduleRecon(Mockito.anyString());

        producer.publish(TradeEvent.created(
                "TRD-DLQ-1",
                JsonNodeFactory.instance.objectNode().put("price", 100)
        ));

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(dlqHas("TRD-DLQ-1")).isTrue());
    }

    private boolean dlqHas(String tradeRef) {
        Properties properties = new Properties();
        properties.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(GROUP_ID_CONFIG, "dlq-assert-" + System.nanoTime());
        properties.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        try (KafkaConsumer<String, TradeEvent> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(KafkaTopicsConfig.TRADE_EVENTS_DLQ));

            for (ConsumerRecord<String, TradeEvent> record : consumer.poll(Duration.ofSeconds(5))) {
                if (tradeRef.equals(record.value().tradeRef())) {
                    return true;
                }
            }
        }

        return false;
    }
}
