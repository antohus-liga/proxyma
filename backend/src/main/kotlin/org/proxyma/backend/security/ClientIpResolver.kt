package org.proxyma.backend.security

import org.proxyma.backend.config.ProxymaProperties
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component

/**
 * Resolves the client address for a request.
 *
 * The connection address is authoritative unless the connection itself comes
 * from a configured trusted proxy (loopback by default, so the Vite dev proxy
 * works out of the box). For a trusted connection, the `X-Forwarded-For` chain
 * is walked from right to left, skipping trusted proxy hops; the first
 * untrusted hop is the client. This ignores any prefix a client may have
 * injected, because the rightmost hops are the ones appended by the proxies we
 * trust. Direct clients cannot spoof the header at all.
 */
@Component
class ClientIpResolver(properties: ProxymaProperties) {

	private val trustedProxies: List<IpAddressMatcher> = properties.http.trustedProxies
		.map { it.trim() }
		.filter { it.isNotEmpty() }
		.map { cidr ->
			runCatching { IpAddressMatcher(cidr) }
				.getOrElse {
					throw IllegalArgumentException(
						"Invalid PROXYMA_HTTP_TRUSTED_PROXIES entry '$cidr'",
						it,
					)
				}
		}

	fun resolve(connectionAddress: String?, forwardedFor: String?): String? {
		if (connectionAddress.isNullOrBlank() || !isTrusted(connectionAddress)) {
			return connectionAddress
		}

		val hops = forwardedFor
			?.split(',')
			.orEmpty()
			.mapNotNull { normalizeLiteral(it.trim()) }

		return hops.asReversed().firstOrNull { !isTrusted(it) } ?: connectionAddress
	}

	private fun isTrusted(address: String): Boolean =
		trustedProxies.any { matcher ->
			runCatching { matcher.matches(address) }.getOrDefault(false)
		}

	/**
	 * Accepts the shapes proxies put in `X-Forwarded-For`: `203.0.113.5`,
	 * `203.0.113.5:1234`, `2001:db8::1` and `[2001:db8::1]:1234`. Only numeric
	 * literals are accepted, so a header value can never trigger DNS traffic.
	 */
	private fun normalizeLiteral(value: String): String? {
		if (value.isEmpty()) {
			return null
		}
		val host = when {
			value.startsWith("[") -> {
				val end = value.indexOf(']')
				if (end < 0) return null
				value.substring(1, end)
			}
			IPV4_WITH_PORT.matches(value) -> value.substringBeforeLast(':')
			else -> value
		}
		return host.takeIf(::isIpLiteral)?.lowercase()
	}

	private fun isIpLiteral(value: String): Boolean =
		IPV4.matches(value) || (value.contains(':') && IPV6.matches(value))

	private companion object {
		private const val IPV4_OCTET = """(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"""
		private val IPV4 = Regex("""^$IPV4_OCTET(?:\.$IPV4_OCTET){3}$""")
		private val IPV4_WITH_PORT = Regex("""^$IPV4_OCTET(?:\.$IPV4_OCTET){3}:\d{1,5}$""")
		private val IPV6 = Regex("""^[0-9A-Fa-f:.]+$""")
	}
}
