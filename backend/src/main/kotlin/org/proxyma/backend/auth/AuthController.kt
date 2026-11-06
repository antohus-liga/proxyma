package org.proxyma.backend.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.proxyma.backend.auth.dto.LoginRequest
import org.proxyma.backend.auth.dto.LoginResponse
import org.proxyma.backend.auth.dto.RegisterRequest
import org.proxyma.backend.common.api.ApiException
import org.proxyma.backend.config.ProxymaProperties
import org.proxyma.backend.security.ratelimit.RateLimitService
import org.proxyma.backend.user.dto.UserResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress
import java.time.Duration

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
	private val authService: AuthService,
	private val refreshCookie: RefreshCookieService,
	private val rateLimit: RateLimitService,
	private val properties: ProxymaProperties,
	private val clock: java.time.Clock,
) {

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	fun register(
		@Valid @RequestBody request: RegisterRequest,
		httpRequest: HttpServletRequest,
		httpResponse: HttpServletResponse,
	): UserResponse {
		val decision = rateLimit.enforce(
			"register:ip",
			clientIp(httpRequest),
			properties.auth.rateLimit.registerPerMinute,
			Duration.ofMinutes(1),
		)
		applyRateLimitHeaders(httpResponse, decision)
		return authService.register(request)
	}

	@PostMapping("/login")
	fun login(
		@Valid @RequestBody request: LoginRequest,
		httpRequest: HttpServletRequest,
		httpResponse: HttpServletResponse,
	): LoginResponse {
		val ip = clientIp(httpRequest)
		val byIp = rateLimit.enforce("login:ip", ip, properties.auth.rateLimit.loginPerMinute, Duration.ofMinutes(1))
		val byAccount = rateLimit.enforce(
			"login:account",
			request.email.trim().lowercase(),
			properties.auth.rateLimit.accountLoginPerMinute,
			Duration.ofMinutes(1),
		)
		applyRateLimitHeaders(httpResponse, byIp, byAccount)

		val result = authService.login(request, httpRequest.getHeader(HttpHeaders.USER_AGENT), inetAddress(ip))
		setRefreshCookie(httpResponse, result)
		return result.response
	}

	@PostMapping("/refresh")
	fun refresh(
		httpRequest: HttpServletRequest,
		httpResponse: HttpServletResponse,
	): LoginResponse {
		val ip = clientIp(httpRequest)
		val decision = rateLimit.enforce(
			"refresh:ip",
			ip,
			properties.auth.rateLimit.refreshPerMinute,
			Duration.ofMinutes(1),
		)
		applyRateLimitHeaders(httpResponse, decision)

		val rawToken = refreshToken(httpRequest)
			?: run {
				httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.clear())
				throw invalidRefreshToken()
			}

		return try {
			val result = authService.refresh(
				rawToken,
				httpRequest.getHeader(HttpHeaders.USER_AGENT),
				inetAddress(ip),
			)
			setRefreshCookie(httpResponse, result)
			result.response
		} catch (ex: ApiException) {
			// A lost race means another request already rotated the cookie. That
			// request's response may have set a valid cookie, so do not clear it.
			if (ex.code != RefreshTokenService.REFRESH_IN_PROGRESS) {
				httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.clear())
			}
			throw ex
		}
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun logout(
		httpRequest: HttpServletRequest,
		httpResponse: HttpServletResponse,
	) {
		refreshToken(httpRequest)?.let { authService.logout(it) }
		httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.clear())
	}

	private fun setRefreshCookie(response: HttpServletResponse, result: AuthService.AuthResult) {
		val maxAge = Duration.between(clock.instant(), result.refreshExpiresAt).coerceAtLeast(Duration.ZERO)
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.create(result.refreshToken, maxAge))
	}

	/**
	 * Advertises the remaining budget of the tightest bucket. Login has two
	 * buckets (per IP and per account), so the client is told about whichever
	 * one is closer to being exhausted.
	 */
	private fun applyRateLimitHeaders(response: HttpServletResponse, vararg decisions: RateLimitService.Decision) {
		val tightest = decisions.minByOrNull { it.remaining } ?: return
		response.setHeader(RateLimitService.HEADER_LIMIT, tightest.limit.toString())
		response.setHeader(RateLimitService.HEADER_REMAINING, tightest.remaining.toString())
		response.setHeader(RateLimitService.HEADER_RESET, tightest.retryAfterSeconds.toString())
	}

	private fun refreshToken(request: HttpServletRequest): String? =
		request.cookies?.firstOrNull { it.name == properties.auth.cookie.name }?.value?.takeIf { it.isNotBlank() }

	private fun clientIp(request: HttpServletRequest): String =
		request.remoteAddr ?: "unknown"

	private fun inetAddress(value: String): InetAddress? =
		runCatching { InetAddress.getByName(value) }.getOrNull()

	private fun invalidRefreshToken() = ApiException(
		status = HttpStatus.UNAUTHORIZED,
		code = "INVALID_REFRESH_TOKEN",
		message = "The refresh token is missing, expired or no longer valid.",
	)
}
