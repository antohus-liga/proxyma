package org.proxyma.backend.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.proxyma.backend.common.api.ProblemResponseWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsUtils
import org.springframework.web.cors.DefaultCorsProcessor

/**
 * Rejects disallowed CORS requests with the same problem+json document the
 * rest of the API uses.
 *
 * The stock processor writes the plain-text string "Invalid CORS request",
 * which was the last remaining non-JSON error path.
 */
class ProblemCorsProcessor(
	private val writer: ProblemResponseWriter,
) : DefaultCorsProcessor() {

	override fun processRequest(
		config: CorsConfiguration?,
		request: HttpServletRequest,
		response: HttpServletResponse,
	): Boolean {
		if (config != null && CorsUtils.isCorsRequest(request)) {
			val origin = request.getHeader("Origin")
			if (config.checkOrigin(origin) == null) {
				writer.write(
					request = request,
					response = response,
					status = HttpServletResponse.SC_FORBIDDEN,
					code = "CORS_REJECTED",
					detail = "The request origin is not allowed.",
				)
				return false
			}
		}
		return super.processRequest(config, request, response)
	}
}
