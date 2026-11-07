package org.proxyma.backend.admin

import org.proxyma.backend.config.ProxymaProperties
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component

/**
 * Optional source-network allowlist for the admin API.
 *
 * The address comes from [org.proxyma.backend.security.ClientIpFilter]: the
 * connection address, unless the connection is from a configured trusted proxy,
 * in which case the forwarded client address is used. A direct client cannot
 * opt itself into the allowlist by sending X-Forwarded-For.
 */
@Component
class AdminNetworkMatcher(properties: ProxymaProperties) {

	private val matchers: List<IpAddressMatcher> = properties.admin.allowedCidrs
		.map { it.trim() }
		.filter { it.isNotEmpty() }
		.map { cidr ->
			runCatching { IpAddressMatcher(cidr) }
				.getOrElse {
					throw IllegalArgumentException(
						"Invalid PROXYMA_ADMIN_ALLOWED_CIDRS entry '$cidr'",
						it,
					)
				}
		}

	fun allows(remoteAddress: String?): Boolean {
		if (matchers.isEmpty()) {
			return true
		}
		if (remoteAddress.isNullOrBlank()) {
			return false
		}
		return matchers.any { matcher ->
			runCatching { matcher.matches(remoteAddress) }.getOrDefault(false)
		}
	}
}
