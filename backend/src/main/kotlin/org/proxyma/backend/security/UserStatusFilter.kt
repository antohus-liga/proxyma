package org.proxyma.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.proxyma.backend.common.api.ProblemResponseWriter
import org.proxyma.backend.user.UserRepository
import org.proxyma.backend.user.UserStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Loads the account behind a valid access token on every request. This is what
 * makes suspension and profile changes take effect immediately, since the token
 * carries no roles or status.
 */
@Component
class UserStatusFilter(
	private val users: UserRepository,
	private val writer: ProblemResponseWriter,
) : OncePerRequestFilter() {

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val authentication = SecurityContextHolder.getContext().authentication
		if (authentication !is JwtAuthenticationToken || !authentication.isAuthenticated) {
			filterChain.doFilter(request, response)
			return
		}

		val userId = runCatching { UUID.fromString(authentication.token.subject) }.getOrNull()
		if (userId == null) {
			SecurityContextHolder.clearContext()
			writer.write(response, 401, "UNAUTHORIZED", "The access token is invalid.")
			return
		}

		val user = users.findById(userId).orElse(null)
		if (user == null) {
			SecurityContextHolder.clearContext()
			writer.write(response, 401, "UNAUTHORIZED", "The access token is invalid.")
			return
		}
		if (user.status == UserStatus.SUSPENDED) {
			SecurityContextHolder.clearContext()
			writer.write(response, 403, "ACCOUNT_SUSPENDED", "This account has been suspended.")
			return
		}

		val principal = ProxymaPrincipal(
			userId = user.id,
			email = user.email,
			displayName = user.displayName,
			platformAdmin = user.platformAdmin,
		)
		val authorities = buildList {
			add(SimpleGrantedAuthority("ROLE_USER"))
			if (user.platformAdmin) {
				add(SimpleGrantedAuthority("PLATFORM_ADMIN"))
			}
		}
		val replacement = UsernamePasswordAuthenticationToken(principal, authentication.token, authorities)
		replacement.details = authentication.details
		SecurityContextHolder.getContext().authentication = replacement

		filterChain.doFilter(request, response)
	}
}
