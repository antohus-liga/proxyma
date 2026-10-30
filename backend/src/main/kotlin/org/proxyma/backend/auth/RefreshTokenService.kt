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
	 */
	// Reuse detection must survive the exception that rejects the request: the
	// family revocation is the whole point, so do not roll it back.
	@Transactional(noRollbackFor = [ApiException::class])
	fun rotate(rawToken: String, userAgent: String?, ip: InetAddress?): Rotation {
		val now = clock.instant()
		val token = repository.findByTokenHashForUpdate(hash(rawToken))
			?: throw invalid()

		if (token.revokedAt != null || token.replacedByTokenId != null) {
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
			expiresAt = token.expiresAt,
		)
		repository.save(replacement)

		token.revokedAt = now
		token.replacedByTokenId = replacement.id
		repository.save(token)

		return Rotation(token.userId, IssuedRefresh(rawReplacement, token.expiresAt))
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

	private fun randomToken(): String {
		val bytes = ByteArray(32)
		random.nextBytes(bytes)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	private fun hash(rawToken: String): String =
		MessageDigest.getInstance("SHA-256")
			.digest(rawToken.toByteArray(Charsets.UTF_8))
			.joinToString("") { "%02x".format(it) }
}
