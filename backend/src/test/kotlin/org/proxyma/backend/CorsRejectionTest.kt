package org.proxyma.backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Browsers still see an opaque CORS failure, but raw clients now get the same
 * problem+json document as every other 4xx instead of the stock plain text
 * "Invalid CORS request".
 */
@TestPropertySource(properties = ["proxyma.cors.allowed-origins=http://localhost:5173"])
class CorsRejectionTest : IntegrationTestBase() {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Test
	fun `a preflight from an allowed origin succeeds`() {
		mockMvc.perform(
			options("/api/v1/auth/login")
				.header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"),
		)
			.andExpect(status().isOk)
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
	}

	@Test
	fun `a preflight from a disallowed origin is a problem document`() {
		mockMvc.perform(
			options("/api/v1/auth/login")
				.header(HttpHeaders.ORIGIN, "http://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"),
		)
			.andExpect(status().isForbidden)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("about:blank"))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Forbidden"))
			.andExpect(jsonPath("$.detail").value("The request origin is not allowed."))
			.andExpect(jsonPath("$.code").value("CORS_REJECTED"))
			.andExpect(jsonPath("$.instance").value("/api/v1/auth/login"))
	}
}
