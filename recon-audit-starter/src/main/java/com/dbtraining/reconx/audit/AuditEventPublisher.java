package com.dbtraining.reconx.audit;

import org.springframework.context.ApplicationEventPublisher;

public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;
    private final AuditProperties properties;

    public AuditEventPublisher(ApplicationEventPublisher publisher, AuditProperties properties) {
        this.publisher = publisher;
        this.properties = properties;
    }

    public AuditProperties properties() {
        return properties;
    }

    public void publish(String action, Object payload) {
        publisher.publishEvent(new AuditEvent(properties.getTopic(), action, payload));
    }

    public record AuditEvent(String topic, String action, Object payload) {}
}
