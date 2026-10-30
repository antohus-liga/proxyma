package org.proxyma.backend.security

import org.proxyma.backend.common.api.ProblemResponseWriter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class RestAccessDeniedHandler(
	private val writer: ProblemResponseWriter,
) : AccessDeniedHandler {

	override fun handle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		accessDeniedException: AccessDeniedException,
	) {
		writer.write(
			response = response,
			status = HttpServletResponse.SC_FORBIDDEN,
			code = "FORBIDDEN",
			detail = "You do not have permission to do that.",
		)
	}
}
