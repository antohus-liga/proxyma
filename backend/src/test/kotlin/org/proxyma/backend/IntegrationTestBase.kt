package org.proxyma.backend

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration::class)
@TestPropertySource(
	properties = [
		"proxyma.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"proxyma.auth.argon2.memory-kibibytes=1024",
		"proxyma.auth.argon2.iterations=1",
		"proxyma.auth.cookie.secure=false",
		"proxyma.auth.rate-limit.register-per-minute=10000",
		"proxyma.auth.rate-limit.login-per-minute=10000",
		"proxyma.auth.rate-limit.account-login-per-minute=10000",
		"proxyma.auth.rate-limit.refresh-per-minute=10000",
	],
)
abstract class IntegrationTestBase
