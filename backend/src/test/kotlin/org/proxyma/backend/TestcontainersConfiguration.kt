package org.proxyma.backend

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * A single PostgreSQL container managed by the Spring context.
 *
 * Declaring the container as a `@Container` field on each test class does not
 * work with Spring's context cache: JUnit stops the container after the class,
 * while the cached context still points at the now-dead port. A context-managed
 * bean lives and dies with the context instead, so every class that shares the
 * context shares the same running database.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	fun postgresContainer(): PostgreSQLContainer =
		PostgreSQLContainer("postgres:17-alpine")
}
