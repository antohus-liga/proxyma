package org.proxyma.backend.security.ratelimit

import org.proxyma.backend.common.api.ApiException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Fixed-window, in-memory rate limiting. Proxyma assumes a single backend
 * instance, so this does not need shared state.
 *
 * [enforce] returns the decision so callers can advertise the remaining budget
 * on successful responses; on rejection it also attaches the same headers to
 * the exception.
 */
@Component
class RateLimitService(
	private val clock: Clock,
) {

	private data class Window(val startsAt: Instant, val duration: Duration, var count: Int)

	private val windows = ConcurrentHashMap<String, Window>()
	private val checks = AtomicLong()

	fun check(scope: String, subject: String, limit: Int, window: Duration): Decision {
		val now = clock.instant()
		val key = "$scope:$subject"

		val state = windows.compute(key) { _, existing ->
			if (existing == null || !now.isBefore(existing.startsAt.plus(existing.duration))) {
				Window(now, window, 1)
			} else {
				existing.count += 1
				existing
			}
		}!!

		maybeCleanUp(now)

		val resetAt = state.startsAt.plus(state.duration)
		val retryAfter = Duration.between(now, resetAt).seconds.coerceAtLeast(1)
		return Decision(
			allowed = state.count <= limit,
			limit = limit,
			remaining = (limit - state.count).coerceAtLeast(0).toLong(),
			retryAfterSeconds = retryAfter,
		)
	}

	fun enforce(scope: String, subject: String, limit: Int, window: Duration = Duration.ofMinutes(1)): Decision {
		val decision = check(scope, subject, limit, window)
		if (!decision.allowed) {
			throw ApiException(
				status = HttpStatus.TOO_MANY_REQUESTS,
				code = "RATE_LIMITED",
				message = "Too many requests. Try again in ${decision.retryAfterSeconds} seconds.",
				headers = decision.headers(),
			)
		}
		return decision
	}

	private fun maybeCleanUp(now: Instant) {
		if (checks.incrementAndGet() % 1024 != 0L) {
			return
		}
		windows.entries.removeIf { (_, window) ->
			!now.isBefore(window.startsAt.plus(window.duration))
		}
	}

	data class Decision(
		val allowed: Boolean,
		val limit: Int,
		val remaining: Long,
		val retryAfterSeconds: Long,
	) {
		fun headers(): HttpHeaders = HttpHeaders().apply {
			set(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
			set(HEADER_LIMIT, limit.toString())
			set(HEADER_REMAINING, remaining.toString())
			set(HEADER_RESET, retryAfterSeconds.toString())
		}
	}

	companion object {
		const val HEADER_LIMIT = "RateLimit-Limit"
		const val HEADER_REMAINING = "RateLimit-Remaining"
		const val HEADER_RESET = "RateLimit-Reset"
	}
}
