package com.flowforge.flowforge;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for all integration tests.
 *
 * @ServiceConnection auto-configures spring.datasource.url/username/password
 * from the running container — no manual property wiring needed.
 *
 * The container is reused across all tests that import this config (singleton pattern).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("flowforge_test")
                .withUsername("test")
                .withPassword("test");
    }
}
