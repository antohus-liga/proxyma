package org.proxyma.backend.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.proxyma.backend.common.api.ProblemResponseWriter
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class RestAuthenticationEntryPoint(
	private val writer: ProblemResponseWriter,
) : AuthenticationEntryPoint {

	override fun commence(
		request: HttpServletRequest,
		response: HttpServletResponse,
		authException: AuthenticationException,
	) {
		writer.write(
			request = request,
			response = response,
			status = HttpServletResponse.SC_UNAUTHORIZED,
			code = "UNAUTHORIZED",
			detail = "Authentication is required or the access token is invalid.",
		)
	}
}
