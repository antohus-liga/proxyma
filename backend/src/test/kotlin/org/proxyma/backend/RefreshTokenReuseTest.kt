package org.proxyma.backend

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.proxyma.backend.auth.RefreshTokenRepository
import org.proxyma.backend.user.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * The concurrency carve-out must be opt-out: with no grace window the old
 * reuse-detection behaviour (revoke the family) still applies.
 */
@TestPropertySource(properties = ["proxyma.auth.refresh-reuse-grace=0s"])
class RefreshTokenReuseTest : IntegrationTestBase() {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var users: UserRepository

	@Autowired
	private lateinit var refreshTokens: RefreshTokenRepository

	@Test
	fun `replaying a rotated token outside the grace window revokes the family`() {
		refreshTokens.deleteAll()
		users.deleteAll()

		val email = "reuse-${UUID.randomUUID()}@example.com"
		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","display_name":"Ana","password":"correct horse battery staple"}"""),
		).andExpect(status().isCreated)

		val login = mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"correct horse battery staple"}"""),
		).andExpect(status().isOk).andReturn()
		val firstCookie = cookieValue(login.response.getHeader(HttpHeaders.SET_COOKIE)!!)

		val refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, firstCookie)))
			.andExpect(status().isOk)
			.andReturn()
		val secondCookie = cookieValue(refreshed.response.getHeader(HttpHeaders.SET_COOKIE)!!)

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, firstCookie)))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))

		// Reuse detection killed the family, including the winner's token.
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, secondCookie)))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
	}

	private fun cookieValue(setCookie: String): String =
		setCookie.substringAfter("$COOKIE_NAME=").substringBefore(";")

	companion object {
		private const val COOKIE_NAME = "proxyma_refresh"
	}
}
