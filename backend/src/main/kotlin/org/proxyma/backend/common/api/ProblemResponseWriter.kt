package org.proxyma.backend.common.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Writes problem+json responses from places that are outside the MVC exception
 * handling pipeline, such as security filters.
 *
 * The body comes from [ApiProblem], the same factory MVC uses, so both paths
 * produce an identical document.
 */
@Component
class ProblemResponseWriter(
	private val objectMapper: ObjectMapper,
) {

	fun write(
		request: HttpServletRequest,
		response: HttpServletResponse,
		status: Int,
		code: String,
		detail: String,
		headers: Map<String, String> = emptyMap(),
	) {
		if (response.isCommitted) {
			return
		}

		response.status = status
		response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
		headers.forEach { (name, value) -> response.setHeader(name, value) }

		objectMapper.writeValue(
			response.outputStream,
			ApiProblem.body(HttpStatus.valueOf(status), code, detail, request.requestURI),
		)
		response.flushBuffer()
	}
}
