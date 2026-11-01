package org.proxyma.backend.admin

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.proxyma.backend.common.api.ProblemResponseWriter
import org.proxyma.backend.config.ProxymaProperties
import org.proxyma.backend.security.ProxymaPrincipal
import org.proxyma.backend.security.ratelimit.RateLimitService
import org.proxyma.backend.user.UserRepository
import org.proxyma.backend.user.UserStatus
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.UUID

/**
 * Authorization gate for `/api/v1/admin`, running after bearer authentication.
 *
 * [AdminAvailabilityFilter] has already handled the disabled-panel and source
 * network checks. This filter turns a valid token into a platform-admin
 * authentication, returns 404 for non-admins, and applies the general admin
 * rate limit.
 */
@Component
class AdminAccessFilter(
	private val properties: ProxymaProperties,
	private val users: UserRepository,
	private val rateLimit: RateLimitService,
	private val writer: ProblemResponseWriter,
) : OncePerRequestFilter() {

	override fun shouldNotFilter(request: HttpServletRequest): Boolean =
		!isAdminPath(request)

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val authentication = SecurityContextHolder.getContext().authentication
		if (authentication !is JwtAuthenticationToken || !authentication.isAuthenticated) {
			unauthorized(request, response)
			return
		}

		val userId = runCatching { UUID.fromString(authentication.token.subject) }.getOrNull()
		if (userId == null) {
			unauthorized(request, response)
			return
		}

		val user = users.findById(userId).orElse(null)
		if (user == null || user.status == UserStatus.SUSPENDED || !user.platformAdmin) {
			notFound(request, response)
			return
		}

		val tokenVersion = (authentication.token.claims["tv"] as? Number)?.toInt() ?: 0
		if (tokenVersion != user.tokenVersion) {
			unauthorized(request, response)
			return
		}

		val decision = rateLimit.check(
			scope = "admin",
			subject = user.id.toString(),
			limit = properties.admin.rateLimit.requestsPerMinute,
			window = Duration.ofMinutes(1),
		)
		if (!decision.allowed) {
			writer.write(
				request = request,
				response = response,
				status = HttpStatus.TOO_MANY_REQUESTS.value(),
				code = "RATE_LIMITED",
				detail = "Too many requests. Try again in ${decision.retryAfterSeconds} seconds.",
				headers = decision.headers().toSingleValueMap(),
			)
			return
		}
		decision.budgetHeaders().forEach { name, values ->
			values.firstOrNull()?.let { response.setHeader(name, it) }
		}

		val principal = ProxymaPrincipal(
			userId = user.id,
			email = user.email,
			displayName = user.displayName,
			platformAdmin = true,
		)
		val replacement = UsernamePasswordAuthenticationToken(
			principal,
			authentication.token,
			listOf(
				SimpleGrantedAuthority("ROLE_USER"),
				SimpleGrantedAuthority(PLATFORM_ADMIN_AUTHORITY),
			),
		)
		replacement.details = authentication.details
		SecurityContextHolder.getContext().authentication = replacement

		filterChain.doFilter(request, response)
	}

	private fun notFound(request: HttpServletRequest, response: HttpServletResponse) {
		SecurityContextHolder.clearContext()
		writer.write(
			request = request,
			response = response,
			status = HttpServletResponse.SC_NOT_FOUND,
			code = "NOT_FOUND",
			detail = "The requested resource does not exist.",
		)
	}

	private fun unauthorized(request: HttpServletRequest, response: HttpServletResponse) {
		SecurityContextHolder.clearContext()
		writer.write(
			request = request,
			response = response,
			status = HttpServletResponse.SC_UNAUTHORIZED,
			code = "UNAUTHORIZED",
			detail = "Authentication is required or the access token is invalid.",
		)
	}

	private fun isAdminPath(request: HttpServletRequest): Boolean {
		val path = request.requestURI.removePrefix(request.contextPath ?: "")
		return path == ADMIN_PATH || path.startsWith("$ADMIN_PATH/")
	}

	companion object {
		const val ADMIN_PATH = "/api/v1/admin"
		const val PLATFORM_ADMIN_AUTHORITY = "PLATFORM_ADMIN"
	}
}
