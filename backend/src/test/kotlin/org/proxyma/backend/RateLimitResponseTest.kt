package org.proxyma.backend

import org.junit.jupiter.api.Test
import org.proxyma.backend.auth.RefreshTokenRepository
import org.proxyma.backend.security.ratelimit.RateLimitService
import org.proxyma.backend.user.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Successful responses advertise the remaining budget, not just rejected ones.
 * The limit is lowered to 1 so a single successful call already shows a zero
 * remainder, which a default-limit test could not observe reliably.
 */
@TestPropertySource(properties = ["proxyma.auth.rate-limit.login-per-minute=1"])
class RateLimitResponseTest : IntegrationTestBase() {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var users: UserRepository

	@Autowired
	private lateinit var refreshTokens: RefreshTokenRepository

	@Test
	fun `a successful login reports the remaining budget`() {
		val email = registerUser()
		val ip = "10.1.0.1"

		mockMvc.perform(
			post("/api/v1/auth/login")
				.with { it.apply { remoteAddr = ip } }
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"$PASSWORD"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(header().string(RateLimitService.HEADER_LIMIT, "1"))
			.andExpect(header().string(RateLimitService.HEADER_REMAINING, "0"))
			.andExpect(header().exists(RateLimitService.HEADER_RESET))
			.andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
	}

	@Test
	fun `a rejected login keeps the rate-limit headers and Retry-After`() {
		val email = registerUser()
		val ip = "10.1.0.2"

		mockMvc.perform(
			post("/api/v1/auth/login")
				.with { it.apply { remoteAddr = ip } }
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"wrong password"}"""),
		)
			.andExpect(status().isUnauthorized)

		mockMvc.perform(
			post("/api/v1/auth/login")
				.with { it.apply { remoteAddr = ip } }
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"wrong password"}"""),
		)
			.andExpect(status().isTooManyRequests)
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
			.andExpect(header().string(RateLimitService.HEADER_LIMIT, "1"))
			.andExpect(header().string(RateLimitService.HEADER_REMAINING, "0"))
			.andExpect(header().exists(RateLimitService.HEADER_RESET))
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
	}

	private fun registerUser(): String {
		refreshTokens.deleteAll()
		users.deleteAll()
		val email = "ratelimit-${UUID.randomUUID()}@example.com"
		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","display_name":"Ana","password":"$PASSWORD"}"""),
		).andExpect(status().isCreated)
		return email
	}

	companion object {
		private const val PASSWORD = "correct horse battery staple"
	}
}
