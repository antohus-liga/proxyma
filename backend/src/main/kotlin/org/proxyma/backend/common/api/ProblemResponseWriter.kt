package org.proxyma.backend.common.api

import tools.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

/**
 * Writes problem+json responses from places that are outside the MVC exception
 * handling pipeline, such as security filters.
 */
@Component
class ProblemResponseWriter(
	private val objectMapper: ObjectMapper,
) {

	fun write(
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

		val body = linkedMapOf(
			"type" to "about:blank",
			"title" to HttpStatusText.titleFor(status),
			"status" to status,
			"code" to code,
			"detail" to detail,
		)
		objectMapper.writeValue(response.outputStream, body)
		response.flushBuffer()
	}
}

private object HttpStatusText {
	fun titleFor(status: Int): String =
		org.springframework.http.HttpStatus.resolve(status)?.reasonPhrase ?: "Error"
}
