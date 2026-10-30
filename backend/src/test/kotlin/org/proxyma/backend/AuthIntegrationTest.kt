package org.proxyma.backend

import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.proxyma.backend.auth.RefreshTokenRepository
import org.proxyma.backend.user.UserRepository
import org.proxyma.backend.user.UserStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class AuthIntegrationTest : IntegrationTestBase() {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var users: UserRepository

	@Autowired
	private lateinit var refreshTokens: RefreshTokenRepository

	@BeforeEach
	fun cleanDatabase() {
		refreshTokens.deleteAll()
		users.deleteAll()
	}

	@Test
	fun `register creates a user and stores an argon2 hash`() {
		val email = uniqueEmail()

		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","display_name":"Ana","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.display_name").value("Ana"))
			.andExpect(jsonPath("$.is_platform_admin").value(false))

		val user = users.findByEmail(email)
		org.junit.jupiter.api.Assertions.assertNotNull(user)
		org.junit.jupiter.api.Assertions.assertTrue(user!!.passwordHash.startsWith("{argon2}"))
	}

	@Test
	fun `register rejects duplicate emails and weak passwords`() {
		val email = uniqueEmail()
		register(email)

		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","display_name":"Ana","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))

		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"${uniqueEmail()}","display_name":"Ana","password":"password"}"""),
		)
			.andExpect(status().`is`(422))
			.andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
	}

	@Test
	fun `login uses a generic error for unknown emails and wrong passwords`() {
		val email = uniqueEmail()
		register(email)

		mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"wrong password"}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))

		mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"nobody@example.com","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
	}

	@Test
	fun `access token authenticates and protected endpoints reject missing tokens`() {
		val email = uniqueEmail()
		register(email)
		val login = login(email)

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken(login)}"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.email").value(email))

		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
	}

	@Test
	fun `refresh rotates the token and reusing the old token revokes the family`() {
		val email = uniqueEmail()
		register(email)
		val login = login(email)
		val firstCookie = refreshCookie(login)

		val refreshed = mockMvc.perform(
			post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, firstCookie)),
		)
			.andExpect(status().isOk)
			.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(COOKIE_NAME)))
			.andReturn()

		val secondCookie = refreshCookie(refreshed)
		org.junit.jupiter.api.Assertions.assertNotEquals(firstCookie, secondCookie)

		// Replaying the first token is treated as theft and revokes the family.
		mockMvc.perform(post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, firstCookie)))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, secondCookie)))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
	}

	@Test
	fun `logout revokes the refresh token`() {
		val email = uniqueEmail()
		register(email)
		val login = login(email)
		val cookie = refreshCookie(login)

		mockMvc.perform(post("/api/v1/auth/logout").cookie(Cookie(COOKIE_NAME, cookie)))
			.andExpect(status().isNoContent)

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, cookie)))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `changing the password revokes every refresh token and changes the password`() {
		val email = uniqueEmail()
		register(email)
		val login = login(email)
		val cookie = refreshCookie(login)

		mockMvc.perform(
			post("/api/v1/users/me/password")
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken(login)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"current_password":"$VALID_PASSWORD","new_password":"$NEW_PASSWORD"}"""),
		)
			.andExpect(status().isNoContent)

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(Cookie(COOKIE_NAME, cookie)))
			.andExpect(status().isUnauthorized)

		mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isUnauthorized)

		login(email, NEW_PASSWORD)
	}

	@Test
	fun `a suspended user cannot log in or use an existing access token`() {
		val email = uniqueEmail()
		register(email)
		val login = login(email)
		val token = accessToken(login)

		val user = users.findByEmail(email)!!
		user.status = UserStatus.SUSPENDED
		user.suspendedAt = java.time.Instant.now()
		users.save(user)

		mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))
	}

	@Test
	fun `profile can be updated`() {
		val email = uniqueEmail()
		register(email)
		val login = login(email)

		mockMvc.perform(
			patch("/api/v1/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken(login)}")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"display_name":"New Name"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.display_name").value("New Name"))

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken(login)}"))
			.andExpect(jsonPath("$.display_name").value("New Name"))
	}

	private fun register(email: String) {
		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","display_name":"Ana","password":"$VALID_PASSWORD"}"""),
		).andExpect(status().isCreated)
	}

	private fun login(email: String, password: String = VALID_PASSWORD): MvcResult =
		mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"$password"}"""),
		).andExpect(status().isOk).andReturn()

	private fun accessToken(result: MvcResult): String =
		com.jayway.jsonpath.JsonPath.read(result.response.contentAsString, "$.access_token")

	private fun refreshCookie(result: MvcResult): String {
		val setCookie = result.response.getHeader(HttpHeaders.SET_COOKIE)
			?: error("Response did not set a refresh cookie")
		return setCookie.substringAfter("$COOKIE_NAME=").substringBefore(";")
	}

	private fun uniqueEmail() = "user-${UUID.randomUUID()}@example.com"

	companion object {
		private const val COOKIE_NAME = "proxyma_refresh"
		private const val VALID_PASSWORD = "correct horse battery staple"
		private const val NEW_PASSWORD = "another very long passphrase"
	}
}
