package com.dbtraining.reconx.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ApplicationEventPublisher.class, () -> event -> {})
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    void autoConfiguresPublisherWhenEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuditEventPublisher.class);
            assertThat(context.getBean(AuditProperties.class).isEnabled()).isTrue();
        });
    }

    @Test
    void backsOffWhenDisabledByProperty() {
        contextRunner
                .withPropertyValues("reconx.audit.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AuditEventPublisher.class));
    }
}
