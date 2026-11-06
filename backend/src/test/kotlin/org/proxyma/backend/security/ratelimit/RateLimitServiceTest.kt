package org.proxyma.backend.security.ratelimit

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimitServiceTest {

	private class MutableClock(var current: Instant) : Clock() {
		override fun instant(): Instant = current
		override fun getZone(): ZoneId = ZoneOffset.UTC
		override fun withZone(zone: ZoneId): Clock = this
	}

	private val clock = MutableClock(Instant.parse("2026-10-30T12:00:00Z"))
	private val limiter = RateLimitService(clock)

	@Test
	fun `allows requests up to the limit and rejects the next one`() {
		val window = Duration.ofMinutes(1)

		assertTrue(limiter.check("login", "127.0.0.1", 2, window).allowed)
		assertTrue(limiter.check("login", "127.0.0.1", 2, window).allowed)
		val denied = limiter.check("login", "127.0.0.1", 2, window)

		assertFalse(denied.allowed)
		assertEquals(60, denied.retryAfterSeconds)
	}

	@Test
	fun `starts a fresh window after it expires`() {
		val window = Duration.ofMinutes(1)
		limiter.check("login", "127.0.0.1", 1, window)
		assertFalse(limiter.check("login", "127.0.0.1", 1, window).allowed)

		clock.current = clock.current.plusSeconds(61)

		assertTrue(limiter.check("login", "127.0.0.1", 1, window).allowed)
	}

	@Test
	fun `tracks subjects separately`() {
		val window = Duration.ofMinutes(1)
		assertTrue(limiter.check("login", "one", 1, window).allowed)
		assertFalse(limiter.check("login", "one", 1, window).allowed)
		assertTrue(limiter.check("login", "two", 1, window).allowed)
	}
}
