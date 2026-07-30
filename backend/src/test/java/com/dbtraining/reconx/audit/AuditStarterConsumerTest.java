package com.dbtraining.reconx.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class AuditStarterConsumerTest {

    @Test
    void starterBeanIsAutoConfiguredOnConsumerClasspath() {
        try (ConfigurableApplicationContext context = createContext()) {
            assertThat(context.getBeansOfType(AuditEventPublisher.class)).hasSize(1);
            AuditEventPublisher publisher = context.getBean(AuditEventPublisher.class);
            assertThat(publisher.properties().getTopic()).isEqualTo("audit-events");
        }
    }

    @Test
    void starterBeanCanBeDisabledByProperty() {
        try (ConfigurableApplicationContext context = createContext("reconx.audit.enabled=false")) {
            assertThat(context.getBeansOfType(AuditEventPublisher.class)).isEmpty();
        }
    }

    private ConfigurableApplicationContext createContext(String... properties) {
        return new SpringApplicationBuilder(TestConsumerApplication.class)
                .web(WebApplicationType.NONE)
                .properties("logging.config=classpath:test-logback.xml")
                .properties(properties)
                .run();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class,
            KafkaAutoConfiguration.class,
            SecurityAutoConfiguration.class
    })
    static class TestConsumerApplication {
    }
}
