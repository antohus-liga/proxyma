package org.proxyma.backend

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `PROXYMA_ADMIN_ALLOWED_CIDRS` is checked before authentication, so a caller
 * outside the allowed networks cannot tell an enabled panel from a missing
 * route, and cannot probe tokens against it either.
 */
@TestPropertySource(
	properties = [
		"proxyma.admin.allowed-cidrs=10.0.0.0/8,192.168.1.0/24,2001:db8::/32",
	],
)
class AdminCidrIntegrationTest : AdminIntegrationTestBase() {

	@Test
	fun `an admin inside an allowed IPv4 network can use the panel`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("10.1.2.3")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		).andExpect(status().isOk)

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("192.168.1.10")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		).andExpect(status().isOk)
	}

	@Test
	fun `an admin inside an allowed IPv6 network can use the panel`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("2001:db8::1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		).andExpect(status().isOk)
	}

	@Test
	fun `a valid admin token from a disallowed network sees not found`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("203.0.113.5")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	@Test
	fun `a disallowed network is rejected before the token is examined`() {
		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("203.0.113.5")
				.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	@Test
	fun `the loopback address is not implicitly allowed`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	@Test
	fun `a trusted proxy can forward an allowed client address`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("127.0.0.1")
				.header("X-Forwarded-For", "10.1.2.3")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		).andExpect(status().isOk)
	}

	@Test
	fun `a trusted proxy cannot forward a disallowed client address`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("127.0.0.1")
				.header("X-Forwarded-For", "203.0.113.5")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	@Test
	fun `an untrusted peer cannot spoof a forwarded address`() {
		val token = adminToken()

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.from("203.0.113.5")
				.header("X-Forwarded-For", "10.1.2.3")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}
}
