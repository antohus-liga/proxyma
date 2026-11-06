package org.proxyma.backend.security.password

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * A small, lowercase blocklist of very common passwords. It is a speed bump,
 * not a substitute for the minimum length rule.
 */
@Component
class CommonPasswords {

	private val passwords: Set<String> = ClassPathResource("security/common-passwords.txt")
		.inputStream
		.bufferedReader()
		.useLines { lines ->
			lines
				.map { it.trim() }
				.filter { it.isNotEmpty() && !it.startsWith("#") }
				.map { it.lowercase() }
				.toSet()
		}

	fun contains(password: String): Boolean = password.lowercase() in passwords
}
