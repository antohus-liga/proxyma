package org.proxyma.backend

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.proxyma.backend.auth.RefreshTokenRepository
import org.proxyma.backend.user.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * The default configuration has the admin panel disabled, so every admin route
 * must look like a route that does not exist — even when the token is broken.
 */
class AdminDisabledIntegrationTest : IntegrationTestBase() {

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
	fun `disabled admin panel returns 404 even with a valid admin token`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	@Test
	fun `disabled admin panel returns 404 before authentication`() {
		mockMvc.perform(get("/api/v1/admin/stats"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	private fun adminToken(): String {
		val email = "disabled-admin-${UUID.randomUUID()}@example.com"
		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"email":"$email","display_name":"Admin","password":"correct horse battery staple"}""",
				),
		).andExpect(status().isCreated)

		val user = users.findByEmail(email)!!
		user.platformAdmin = true
		users.save(user)

		val login = mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"correct horse battery staple"}"""),
		).andExpect(status().isOk).andReturn()

		return JsonPath.read(login.response.contentAsString, "$.access_token")
	}
}
