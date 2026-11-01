package org.proxyma.backend.admin

import org.proxyma.backend.user.User
import org.proxyma.backend.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Server-side operator commands. There is deliberately no HTTP endpoint for
 * this: the platform-admin flag is not something the product can grant.
 */
@Service
class PlatformAdminService(
	private val users: UserRepository,
	private val audit: AdminAuditWriter,
) {

	data class Result(val user: User, val changed: Boolean)

	@Transactional
	fun grant(email: String): Result = setFlag(email, true)

	@Transactional
	fun revoke(email: String): Result = setFlag(email, false)

	private fun setFlag(email: String, value: Boolean): Result {
		val user = users.findByEmail(email.trim())
			?: throw IllegalArgumentException("No user with email '$email'.")

		if (user.platformAdmin == value) {
			return Result(user, changed = false)
		}

		user.platformAdmin = value
		// Existing access tokens must not silently acquire admin powers, and a
		// revoked admin's current token must stop working immediately.
		user.tokenVersion += 1
		users.save(user)

		val verb = if (value) "granted" else "revoked"
		audit.recordPlatform(
			adminUserId = null,
			action = if (value) "platform_admin.grant" else "platform_admin.revoke",
			targetType = "user",
			targetId = user.id.toString(),
			summary = "Operator $verb platform admin for ${user.email}",
			sourceIp = null,
		)

		return Result(user, changed = true)
	}
}
