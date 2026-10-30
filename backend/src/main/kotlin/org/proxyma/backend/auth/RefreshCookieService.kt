package org.proxyma.backend.auth

import org.proxyma.backend.config.ProxymaProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshCookieService(
	private val properties: ProxymaProperties,
) {

	fun create(value: String, maxAge: Duration): String {
		val cookie = properties.auth.cookie
		val builder = ResponseCookie.from(cookie.name, value)
			.httpOnly(true)
			.secure(cookie.secure)
			.sameSite(cookie.sameSite)
			.path(cookie.path)
			.maxAge(maxAge)
		if (!cookie.domain.isNullOrBlank()) {
			builder.domain(cookie.domain)
		}
		return builder.build().toString()
	}

	fun clear(): String {
		val cookie = properties.auth.cookie
		val builder = ResponseCookie.from(cookie.name, "")
			.httpOnly(true)
			.secure(cookie.secure)
			.sameSite(cookie.sameSite)
			.path(cookie.path)
			.maxAge(Duration.ZERO)
		if (!cookie.domain.isNullOrBlank()) {
			builder.domain(cookie.domain)
		}
		return builder.build().toString()
	}
}
