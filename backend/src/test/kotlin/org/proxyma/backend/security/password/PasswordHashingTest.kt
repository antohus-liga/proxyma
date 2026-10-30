package org.proxyma.backend.security.password

import org.junit.jupiter.api.Test
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHashingTest {

	private val encoder = DelegatingPasswordEncoder(
		"argon2",
		mapOf("argon2" to Argon2PasswordEncoder(16, 32, 1, 1024, 1)),
	)

	@Test
	fun `hashes are tagged with the argon2 encoding id`() {
		val hash = encoder.encode("correct horse battery staple")!!
		assertTrue(hash.startsWith($$"{argon2}$argon2id$"))
		assertTrue(encoder.matches("correct horse battery staple", hash))
		assertFalse(encoder.matches("wrong", hash))
	}
}
