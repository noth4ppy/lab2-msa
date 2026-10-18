package io.hexlet.spring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Auditing (@CreatedDate / @LastModifiedDate) is enabled in a separate configuration, not on the
 * main class: otherwise @WebMvcTest slices fail with "JPA metamodel must not be empty".
 * Slice tests that need auditing (@DataJpaTest) import this class explicitly.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
