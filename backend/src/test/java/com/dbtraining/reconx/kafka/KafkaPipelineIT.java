package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class KafkaPipelineIT {
    @SuppressWarnings("deprecation")
    @Container
    private static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void kafkaProp(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TradeEventProducer producer;
    @Autowired
    private AuditLogRepository auditRepo;

    public void testKafkaPipeline() {
        // Here you can implement your integration test logic.
        // For example, you can publish a TradeEvent and verify that it is consumed and
        // processed correctly.
        // You can also check the AuditLogRepository to ensure that the audit log
        // entries are created as expected.

        // get time
        long before = auditRepo.count();

        // create 100 trade events and publish them to Kafka
        IntStream.range(0, 100).forEach(i -> producer.publish(TradeEvent.created(
                "TRD-IT-" + i,
                JsonNodeFactory.instance.objectNode().put("price", i))));

        // await for the audit log to be updated with the new events
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(auditRepo.count()).isEqualTo(before + 100));

    }

}
