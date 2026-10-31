package org.proxyma.backend.config

import java.time.Duration

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "proxyma")
data class ProxymaProperties(
	val jwt: JwtProperties = JwtProperties(),
	val auth: AuthProperties = AuthProperties(),
	val cors: CorsProperties = CorsProperties(),
)

data class JwtProperties(
	val secret: String = "",
	val issuer: String = "proxyma",
)

data class AuthProperties(
	val accessTokenTtl: Duration = Duration.ofMinutes(15),
	val adminAccessTokenTtl: Duration = Duration.ofMinutes(5),
	val refreshTokenTtl: Duration = Duration.ofDays(7),
	/** How long a just-rotated refresh token is treated as a race, not theft. */
	val refreshReuseGrace: Duration = Duration.ofSeconds(30),
	val cookie: CookieProperties = CookieProperties(),
	val password: PasswordProperties = PasswordProperties(),
	val argon2: Argon2Properties = Argon2Properties(),
	val rateLimit: RateLimitProperties = RateLimitProperties(),
)

data class CookieProperties(
	val name: String = "proxyma_refresh",
	val path: String = "/api/v1/auth",
	val secure: Boolean = true,
	val sameSite: String = "Strict",
	val domain: String? = null,
)

data class PasswordProperties(
	val minLength: Int = 10,
	val maxLength: Int = 128,
)

data class Argon2Properties(
	val saltLength: Int = 16,
	val hashLength: Int = 32,
	val parallelism: Int = 1,
	val memoryKibibytes: Int = 19_456,
	val iterations: Int = 2,
)

data class RateLimitProperties(
	val registerPerMinute: Int = 10,
	val loginPerMinute: Int = 10,
	val accountLoginPerMinute: Int = 10,
	val refreshPerMinute: Int = 30,
)

data class CorsProperties(
	val allowedOrigins: List<String> = emptyList(),
)
