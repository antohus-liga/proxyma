package org.proxyma.backend.security.password

import org.proxyma.backend.config.ProxymaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class PasswordConfig {

	@Bean
	fun passwordEncoder(properties: ProxymaProperties): PasswordEncoder {
		val argon2Properties = properties.auth.argon2
		val argon2 = Argon2PasswordEncoder(
			argon2Properties.saltLength,
			argon2Properties.hashLength,
			argon2Properties.parallelism,
			argon2Properties.memoryKibibytes,
			argon2Properties.iterations,
		)
		// The encoding id is stored in the hash, so another algorithm can be
		// introduced later without invalidating existing passwords.
		return DelegatingPasswordEncoder("argon2", mapOf("argon2" to argon2))
	}
}
