package org.proxyma.backend

import org.junit.jupiter.api.Test
import org.proxyma.backend.user.UserStatus
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

class AdminAccessIntegrationTest : AdminIntegrationTestBase() {

	@Test
	fun `admin endpoints require a valid access token`() {
		mockMvc.perform(get("/api/v1/admin/stats"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.instance").value("/api/v1/admin/stats"))
	}

	@Test
	fun `a normal user sees the admin namespace as not found`() {
		val email = uniqueEmail()
		createUser(email)
		val token = accessToken(login(email))

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	@Test
	fun `a suspended platform admin sees not found rather than account suspended`() {
		val email = uniqueEmail()
		createUser(email, platformAdmin = true)
		val token = accessToken(login(email))

		val user = users.findByEmail(email)!!
		user.status = UserStatus.SUSPENDED
		user.suspendedAt = Instant.now()
		users.save(user)

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	@Test
	fun `a platform admin can reach the admin namespace and sees the rate limit budget`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(header().exists("RateLimit-Limit"))
			.andExpect(header().exists("RateLimit-Remaining"))
			.andExpect(header().exists("RateLimit-Reset"))
			.andExpect(jsonPath("$.users.total").value(1))
			.andExpect(jsonPath("$.orgs.total").value(0))
	}

	@Test
	fun `an admin flag revoked after login takes effect immediately`() {
		val email = uniqueEmail()
		createUser(email, platformAdmin = true)
		val token = accessToken(login(email))

		val user = users.findByEmail(email)!!
		user.platformAdmin = false
		users.save(user)

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	private fun uniqueEmail() = "admin-access-${UUID.randomUUID()}@example.com"
}
