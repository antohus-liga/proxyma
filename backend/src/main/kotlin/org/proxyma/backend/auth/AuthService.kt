package org.proxyma.backend.auth

import org.proxyma.backend.auth.dto.LoginRequest
import org.proxyma.backend.auth.dto.LoginResponse
import org.proxyma.backend.auth.dto.RegisterRequest
import org.proxyma.backend.common.api.ApiException
import org.proxyma.backend.security.JwtService
import org.proxyma.backend.security.password.PasswordPolicy
import org.proxyma.backend.user.User
import org.proxyma.backend.user.UserRepository
import org.proxyma.backend.user.UserStatus
import org.proxyma.backend.user.dto.ChangePasswordRequest
import org.proxyma.backend.user.dto.UserResponse
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.time.Clock
import java.util.UUID

@Service
class AuthService(
	private val users: UserRepository,
	private val passwordEncoder: PasswordEncoder,
	private val passwordPolicy: PasswordPolicy,
	private val jwtService: JwtService,
	private val refreshTokens: RefreshTokenService,
	private val clock: Clock,
) {

	// Argon2 is run even when the account does not exist, so response time does
	// not reveal whether an email is registered.
	private val dummyHash: String = requireNotNull(passwordEncoder.encode(UUID.randomUUID().toString()))

	data class AuthResult(
		val response: LoginResponse,
		val refreshToken: String,
		val refreshExpiresAt: java.time.Instant,
	)

	@Transactional
	fun register(request: RegisterRequest): UserResponse {
		val email = normalizeEmail(request.email)
		passwordPolicy.validateOrThrow(request.password, "password")

		if (users.existsByEmail(email)) {
			throw emailTaken()
		}

		val user = User(
			email = email,
			passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
			displayName = request.displayName.trim(),
		)
		val saved = try {
			users.saveAndFlush(user)
		} catch (ex: org.springframework.dao.DataIntegrityViolationException) {
			throw emailTaken()
		}
		return UserResponse.from(saved)
	}

	@Transactional
	fun login(request: LoginRequest, userAgent: String?, ip: InetAddress?): AuthResult {
		val email = normalizeEmail(request.email)
		val user = users.findByEmail(email)
		val encoded: String = user?.passwordHash ?: dummyHash
		val matches = passwordEncoder.matches(request.password, encoded)

		if (user == null || !matches) {
			throw invalidCredentials()
		}
		if (user.status == UserStatus.SUSPENDED) {
			throw accountSuspended()
		}

		user.lastLoginAt = clock.instant()
		users.save(user)

		return issue(user, userAgent, ip)
	}

	fun refresh(rawRefreshToken: String, userAgent: String?, ip: InetAddress?): AuthResult {
		val rotation = refreshTokens.rotate(rawRefreshToken, userAgent, ip)
		val user = users.findById(rotation.userId).orElse(null)
		if (user == null) {
			refreshTokens.revokeAllForUser(rotation.userId)
			throw invalidRefreshToken()
		}
		if (user.status == UserStatus.SUSPENDED) {
			refreshTokens.revokeAllForUser(user.id)
			throw accountSuspended()
		}

		val access = jwtService.issueAccessToken(user)
		return AuthResult(
			response = LoginResponse(
				accessToken = access.value,
				expiresIn = access.expiresInSeconds,
				user = UserResponse.from(user),
			),
			refreshToken = rotation.refresh.rawToken,
			refreshExpiresAt = rotation.refresh.expiresAt,
		)
	}

	@Transactional
	fun changePassword(userId: UUID, request: ChangePasswordRequest) {
		val user = users.findById(userId).orElseThrow { invalidCredentials() }
		if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
			throw ApiException(
				status = HttpStatus.FORBIDDEN,
				code = "REAUTH_FAILED",
				message = "The current password is incorrect.",
			)
		}
		passwordPolicy.validateOrThrow(request.newPassword, "new_password")
		user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword))
		user.tokenVersion += 1
		users.save(user)
		refreshTokens.revokeAllForUser(user.id)
	}

	@Transactional
	fun updateDisplayName(userId: UUID, displayName: String): UserResponse {
		val user = users.findById(userId).orElseThrow { invalidCredentials() }
		user.displayName = displayName.trim()
		return UserResponse.from(users.save(user))
	}

	@Transactional
	fun suspend(userId: UUID, actorUserId: UUID, reason: String) {
		val user = users.findById(userId).orElseThrow { notFound() }
		val now = clock.instant()
		user.status = UserStatus.SUSPENDED
		user.suspendedAt = now
		user.suspendedByUserId = actorUserId
		user.suspensionReason = reason
		user.tokenVersion += 1
		users.save(user)
		refreshTokens.revokeAllForUser(user.id)
	}

	@Transactional
	fun unsuspend(userId: UUID) {
		val user = users.findById(userId).orElseThrow { notFound() }
		user.status = UserStatus.ACTIVE
		user.suspendedAt = null
		user.suspendedByUserId = null
		user.suspensionReason = null
		// Do not resurrect tokens issued before the suspension.
		user.tokenVersion += 1
		users.save(user)
	}

	@Transactional
	fun forceLogout(userId: UUID) {
		val user = users.findById(userId).orElseThrow { notFound() }
		user.tokenVersion += 1
		users.save(user)
		refreshTokens.revokeAllForUser(userId)
	}

	fun logout(rawRefreshToken: String) {
		refreshTokens.revokeByRawToken(rawRefreshToken)
	}

	private fun issue(user: User, userAgent: String?, ip: InetAddress?): AuthResult {
		val access = jwtService.issueAccessToken(user)
		val refresh = refreshTokens.issue(
			userId = user.id,
			userAgent = userAgent,
			ip = ip,
		)
		return AuthResult(
			response = LoginResponse(
				accessToken = access.value,
				expiresIn = access.expiresInSeconds,
				user = UserResponse.from(user),
			),
			refreshToken = refresh.rawToken,
			refreshExpiresAt = refresh.expiresAt,
		)
	}

	private fun normalizeEmail(email: String): String = email.trim().lowercase()

	private fun emailTaken() = ApiException(
		status = HttpStatus.CONFLICT,
		code = "EMAIL_TAKEN",
		message = "An account with that email already exists.",
	)

	private fun invalidCredentials() = ApiException(
		status = HttpStatus.UNAUTHORIZED,
		code = "INVALID_CREDENTIALS",
		message = "The email or password is incorrect.",
	)

	private fun accountSuspended() = ApiException(
		status = HttpStatus.FORBIDDEN,
		code = "ACCOUNT_SUSPENDED",
		message = "This account has been suspended.",
	)

	private fun invalidRefreshToken() = ApiException(
		status = HttpStatus.UNAUTHORIZED,
		code = "INVALID_REFRESH_TOKEN",
		message = "The refresh token is missing, expired or no longer valid.",
	)

	private fun notFound() = ApiException(
		status = HttpStatus.NOT_FOUND,
		code = "NOT_FOUND",
		message = "The requested user does not exist.",
	)
}
