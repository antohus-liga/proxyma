package org.proxyma.backend

import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * The admin API has two budgets: one for every admin request and a stricter
 * one for the risky actions. Both are keyed by admin, so a fresh admin per test
 * starts with a full budget.
 */
@TestPropertySource(
	properties = [
		"proxyma.admin.rate-limit.requests-per-minute=5",
		"proxyma.admin.rate-limit.risky-per-minute=2",
	],
)
class AdminRateLimitIntegrationTest : AdminIntegrationTestBase() {

	@Test
	fun `the general admin budget returns 429 with retry headers once exhausted`() {
		val token = adminToken()

		repeat(5) { index ->
			mockMvc.perform(
				get("/api/v1/admin/stats")
					.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
			)
				.andExpect(status().isOk)
				.andExpect(header().string("RateLimit-Limit", "5"))
				.andExpect(header().string("RateLimit-Remaining", (4 - index).toString()))
		}

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isTooManyRequests)
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
			.andExpect(header().string("RateLimit-Limit", "5"))
			.andExpect(header().string("RateLimit-Remaining", "0"))
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
	}

	@Test
	fun `the risky budget is stricter than the general budget`() {
		val token = adminToken()
		val targetId = createUser(uniqueEmail("target"), "Target User")

		repeat(2) {
			mockMvc.perform(
				post("/api/v1/admin/users/$targetId/force-logout")
					.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"reason":"test","password":"$VALID_PASSWORD"}"""),
			)
				.andExpect(status().isNoContent)
		}

		mockMvc.perform(
			post("/api/v1/admin/users/$targetId/force-logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"test","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isTooManyRequests)
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
	}

	@Test
	fun `failed step-up attempts still consume the risky budget`() {
		val token = adminToken()
		val targetId = createUser(uniqueEmail("target"), "Target User")

		// Two wrong-password attempts still consume the risky budget, because
		// the point of the limit is to slow down password guessing.
		repeat(2) {
			mockMvc.perform(
				post("/api/v1/admin/users/$targetId/force-logout")
					.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"reason":"test","password":"wrong password"}"""),
			).andExpect(status().isForbidden)
		}

		mockMvc.perform(
			post("/api/v1/admin/users/$targetId/force-logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"test","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isTooManyRequests)
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"))
	}

	@Test
	fun `different admins have independent budgets`() {
		val first = adminToken()
		val second = adminToken()

		repeat(5) {
			mockMvc.perform(
				get("/api/v1/admin/stats")
					.header(HttpHeaders.AUTHORIZATION, "Bearer $first"),
			).andExpect(status().isOk)
		}

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $first"),
		).andExpect(status().isTooManyRequests)

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $second"),
		)
			.andExpect(status().isOk)
			.andExpect(header().string("RateLimit-Remaining", "4"))
	}

	private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"
}
