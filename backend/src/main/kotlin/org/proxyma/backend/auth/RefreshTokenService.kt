package org.proxyma.backend.auth

import org.proxyma.backend.common.api.ApiException
import org.proxyma.backend.config.ProxymaProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class RefreshTokenService(
	private val repository: RefreshTokenRepository,
	private val properties: ProxymaProperties,
	private val clock: Clock,
) {

	private val random = SecureRandom()

	data class IssuedRefresh(val rawToken: String, val expiresAt: Instant)

	data class Rotation(val userId: UUID, val refresh: IssuedRefresh)

	@Transactional
	fun issue(
		userId: UUID,
		familyId: UUID = UUID.randomUUID(),
		familyExpiresAt: Instant? = null,
		userAgent: String? = null,
		ip: InetAddress? = null,
	): IssuedRefresh {
		val now = clock.instant()
		val expiresAt = familyExpiresAt ?: now.plus(properties.auth.refreshTokenTtl)
		val raw = randomToken()
		repository.save(
			RefreshToken(
				userId = userId,
				tokenHash = hash(raw),
				familyId = familyId,
				userAgent = userAgent?.take(512),
				ip = ip,
				expiresAt = expiresAt,
			),
		)
		return IssuedRefresh(raw, expiresAt)
	}

	/**
	 * Rotates a refresh token. Presenting an already-revoked token is treated as
	 * theft: the whole family is revoked.
	 *
	 * The exception is a token that was rotated moments ago. Two concurrent
	 * refresh requests from the same client would otherwise trip reuse detection
	 * and log the user out, so a replay within [ProxymaProperties]'
	 * `refresh-reuse-grace` window fails with [REFRESH_IN_PROGRESS] and leaves
	 * the family intact. The loser of the race still gets no token; the winner's
	 * rotation stands.
	 */
	// Reuse detection must survive the exception that rejects the request: the
	// family revocation is the whole point, so do not roll it back.
	@Transactional(noRollbackFor = [ApiException::class])
	fun rotate(rawToken: String, userAgent: String?, ip: InetAddress?): Rotation {
		val now = clock.instant()
		val token = repository.findByTokenHashForUpdate(hash(rawToken))
			?: throw invalid()

		if (token.revokedAt != null || token.replacedByTokenId != null) {
			if (wasJustRotated(token, now)) {
				throw refreshInProgress()
			}
			repository.revokeFamily(token.familyId, now)
			throw invalid()
		}
		if (!token.expiresAt.isAfter(now)) {
			repository.revokeFamily(token.familyId, now)
			throw invalid()
		}

		val rawReplacement = randomToken()
		val replacement = RefreshToken(
			userId = token.userId,
			tokenHash = hash(rawReplacement),
			familyId = token.familyId,
			userAgent = userAgent?.take(512),
			ip = ip,
			// Sliding expiry: every rotation renews the token for a full TTL.
			expiresAt = now.plus(properties.auth.refreshTokenTtl),
		)
		repository.save(replacement)

		token.revokedAt = now
		token.replacedByTokenId = replacement.id
		repository.save(token)

		// Report the replacement's expiry, not the one it replaced: the cookie's
		// Max-Age must slide with the token, otherwise it expires on the old date.
		return Rotation(token.userId, IssuedRefresh(rawReplacement, replacement.expiresAt))
	}

	@Transactional
	fun revokeByRawToken(rawToken: String) {
		val now = clock.instant()
		val token = repository.findByTokenHashForUpdate(hash(rawToken)) ?: return
		repository.revokeFamily(token.familyId, now)
	}

	@Transactional
	fun revokeAllForUser(userId: UUID) {
		repository.revokeAllForUser(userId, clock.instant())
	}

	fun remainingLifetime(expiresAt: Instant): Duration =
		Duration.between(clock.instant(), expiresAt).coerceAtLeast(Duration.ZERO)

	private fun invalid() = ApiException(
		status = HttpStatus.UNAUTHORIZED,
		code = "INVALID_REFRESH_TOKEN",
		message = "The refresh token is missing, expired or no longer valid.",
	)

	/**
	 * True when this token was rotated inside the grace window, which means the
	 * caller most likely lost a race with its own concurrent refresh rather than
	 * replaying a stolen token. A zero grace disables the carve-out entirely.
	 */
	private fun wasJustRotated(token: RefreshToken, now: Instant): Boolean {
		val grace = properties.auth.refreshReuseGrace
		if (grace.isZero || grace.isNegative) {
			return false
		}
		if (token.replacedByTokenId == null || token.revokedAt == null) {
			return false
		}
		return Duration.between(token.revokedAt, now) <= grace
	}

	private fun refreshInProgress() = ApiException(
		status = HttpStatus.CONFLICT,
		code = REFRESH_IN_PROGRESS,
		message = "This refresh token was already rotated. Retry with the current cookie.",
	)

	private fun randomToken(): String {
		val bytes = ByteArray(32)
		random.nextBytes(bytes)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	private fun hash(rawToken: String): String =
		MessageDigest.getInstance("SHA-256")
			.digest(rawToken.toByteArray(Charsets.UTF_8))
			.joinToString("") { "%02x".format(it) }

	companion object {
		const val REFRESH_IN_PROGRESS = "REFRESH_IN_PROGRESS"
	}
}
