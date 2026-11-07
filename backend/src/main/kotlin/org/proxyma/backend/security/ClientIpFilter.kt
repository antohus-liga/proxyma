package org.proxyma.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Replaces [HttpServletRequest.getRemoteAddr] with the address resolved by
 * [ClientIpResolver], so filters, rate limiting and audit logging all see the
 * real client even when the API is reached through a trusted reverse proxy.
 *
 * Registered before the Spring Security chain so every downstream check uses
 * the resolved address.
 */
@Component
class ClientIpFilter(
	private val resolver: ClientIpResolver,
) : OncePerRequestFilter() {

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val resolved = resolver.resolve(
			connectionAddress = request.remoteAddr,
			forwardedFor = request.getHeader(FORWARDED_FOR_HEADER),
		)
		if (resolved == null || resolved == request.remoteAddr) {
			filterChain.doFilter(request, response)
		} else {
			filterChain.doFilter(ClientAddressRequest(request, resolved), response)
		}
	}

	private class ClientAddressRequest(
		request: HttpServletRequest,
		private val clientAddress: String,
	) : HttpServletRequestWrapper(request) {
		override fun getRemoteAddr(): String = clientAddress
	}

	companion object {
		const val FORWARDED_FOR_HEADER = "X-Forwarded-For"
	}
}
