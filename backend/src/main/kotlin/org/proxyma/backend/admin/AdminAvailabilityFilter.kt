package org.proxyma.backend.admin

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.proxyma.backend.common.api.ProblemResponseWriter
import org.proxyma.backend.config.ProxymaProperties
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs before bearer authentication so that a disabled panel or a disallowed
 * source network is indistinguishable from a route that does not exist, even
 * for a request with a broken token.
 */
@Component
class AdminAvailabilityFilter(
	private val properties: ProxymaProperties,
	private val networkMatcher: AdminNetworkMatcher,
	private val writer: ProblemResponseWriter,
) : OncePerRequestFilter() {

	override fun shouldNotFilter(request: HttpServletRequest): Boolean =
		!isAdminPath(request)

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		if (!properties.admin.enabled || !networkMatcher.allows(request.remoteAddr)) {
			writer.write(
				request = request,
				response = response,
				status = HttpServletResponse.SC_NOT_FOUND,
				code = "NOT_FOUND",
				detail = "The requested resource does not exist.",
			)
			return
		}
		filterChain.doFilter(request, response)
	}

	private fun isAdminPath(request: HttpServletRequest): Boolean {
		val path = request.requestURI.removePrefix(request.contextPath ?: "")
		return path == AdminAccessFilter.ADMIN_PATH ||
			path.startsWith("${AdminAccessFilter.ADMIN_PATH}/")
	}
}
