package com.dbtraining.reconx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * TICKET-ADV050 — @CreatedDate / @LastModifiedDate population.
 *
 * Kept in its own @Configuration class rather than on ReconxApplication:
 * @WebMvcTest and other slice tests import the @SpringBootApplication class
 * directly, so an @EnableJpaAuditing there leaks a jpaAuditingHandler bean
 * that needs a real JPA metamodel into slices that have none.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
