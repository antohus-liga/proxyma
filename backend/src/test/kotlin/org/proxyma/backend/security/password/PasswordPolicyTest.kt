package org.proxyma.backend.security.password

import org.junit.jupiter.api.Test
import org.proxyma.backend.config.ProxymaProperties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordPolicyTest {

	private val policy = PasswordPolicy(ProxymaProperties(), CommonPasswords())

	@Test
	fun `rejects short passwords`() {
		val errors = policy.validate("short")
		assertTrue(errors.any { it.message.contains("at least 10") })
	}

	@Test
	fun `rejects common passwords even when long enough`() {
		val errors = policy.validate("password")
		assertTrue(errors.any { it.message.contains("too common") })
	}

	@Test
	fun `accepts a long passphrase`() {
		assertEquals(emptyList(), policy.validate("correct horse battery staple"))
	}
}
