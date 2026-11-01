package org.proxyma.backend.admin

import org.proxyma.backend.admin.dto.AdminAuditListResponse
import org.proxyma.backend.admin.dto.AdminForceLogoutRequest
import org.proxyma.backend.admin.dto.AdminOrgDetail
import org.proxyma.backend.admin.dto.AdminOrgListResponse
import org.proxyma.backend.admin.dto.AdminStatsResponse
import org.proxyma.backend.admin.dto.AdminSuspendRequest
import org.proxyma.backend.admin.dto.AdminUnsuspendRequest
import org.proxyma.backend.admin.dto.AdminUserDetail
import org.proxyma.backend.admin.dto.AdminUserListResponse
import org.proxyma.backend.auth.AuthService
import org.proxyma.backend.common.api.ApiException
import org.proxyma.backend.user.User
import org.proxyma.backend.user.UserRepository
import org.proxyma.backend.user.UserStatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AdminService(
	private val users: UserRepository,
	private val authService: AuthService,
	private val query: AdminQueryRepository,
	private val audit: AdminAuditWriter,
) {

	fun stats(): AdminStatsResponse = query.stats()

	fun listUsers(
		q: String?,
		status: String?,
		cursor: AdminCursor?,
		limit: Int,
	): AdminUserListResponse {
		val normalizedStatus = status?.trim()?.takeIf { it.isNotEmpty() }?.let {
			UserStatus.entries.firstOrNull { value -> value.value == it }
				?.value
				?: throw invalidParameter("The parameter 'status' must be 'active' or 'suspended'.")
		}
		val (items, nextCursor) = query.searchUsers(
			q = q,
			status = normalizedStatus,
			cursor = cursor,
			limit = limit,
		)
		return AdminUserListResponse(items = items, nextCursor = nextCursor)
	}

	@Transactional
	fun getUser(adminUserId: UUID, userId: UUID, sourceIp: String?): AdminUserDetail {
		val detail = query.findUserDetail(userId) ?: throw userNotFound()
		audit.recordPlatform(
			adminUserId = adminUserId,
			action = "user.view",
			targetType = "user",
			targetId = userId.toString(),
			summary = "Viewed user ${detail.email}",
			sourceIp = sourceIp,
		)
		return detail
	}

	@Transactional
	fun suspend(
		adminUserId: UUID,
		userId: UUID,
		request: AdminSuspendRequest,
		sourceIp: String?,
	): AdminUserDetail {
		val target = findTarget(userId)
		if (target.id == adminUserId) {
			throw ApiException(
				status = HttpStatus.CONFLICT,
				code = "CANNOT_SUSPEND_SELF",
				message = "You cannot suspend your own account.",
			)
		}
		if (target.platformAdmin) {
			throw ApiException(
				status = HttpStatus.FORBIDDEN,
				code = "ADMIN_CANNOT_TARGET_ADMIN",
				message = "A platform administrator cannot suspend another platform administrator.",
			)
		}

		authService.verifyPassword(adminUserId, request.password)
		val reason = request.reason.trim()
		val orgIds = query.orgIdsForUser(target.id)

		authService.suspend(target.id, adminUserId, reason)

		orgIds.forEach { orgId ->
			audit.recordOrg(
				orgId = orgId,
				adminUserId = adminUserId,
				action = "member.suspend",
				targetType = "user",
				targetId = target.id.toString(),
				summary = "Member ${target.displayName} (${target.email}) was suspended by a platform administrator: $reason",
				sourceIp = sourceIp,
			)
		}
		audit.recordPlatform(
			adminUserId = adminUserId,
			action = "user.suspend",
			targetType = "user",
			targetId = target.id.toString(),
			summary = "Suspended ${target.email}: $reason",
			sourceIp = sourceIp,
		)

		return query.findUserDetail(target.id) ?: throw userNotFound()
	}

	@Transactional
	fun unsuspend(
		adminUserId: UUID,
		userId: UUID,
		request: AdminUnsuspendRequest,
		sourceIp: String?,
	): AdminUserDetail {
		val target = findTarget(userId)
		val reason = request.reason.trim()
		val orgIds = query.orgIdsForUser(target.id)

		authService.unsuspend(target.id)

		orgIds.forEach { orgId ->
			audit.recordOrg(
				orgId = orgId,
				adminUserId = adminUserId,
				action = "member.unsuspend",
				targetType = "user",
				targetId = target.id.toString(),
				summary = "Member ${target.displayName} (${target.email}) was unsuspended by a platform administrator: $reason",
				sourceIp = sourceIp,
			)
		}
		audit.recordPlatform(
			adminUserId = adminUserId,
			action = "user.unsuspend",
			targetType = "user",
			targetId = target.id.toString(),
			summary = "Unsuspended ${target.email}: $reason",
			sourceIp = sourceIp,
		)

		return query.findUserDetail(target.id) ?: throw userNotFound()
	}

	@Transactional
	fun forceLogout(
		adminUserId: UUID,
		userId: UUID,
		request: AdminForceLogoutRequest,
		sourceIp: String?,
	) {
		val target = findTarget(userId)
		authService.verifyPassword(adminUserId, request.password)
		val reason = request.reason.trim()

		authService.forceLogout(target.id)
		audit.recordPlatform(
			adminUserId = adminUserId,
			action = "user.force_logout",
			targetType = "user",
			targetId = target.id.toString(),
			summary = "Forced logout for ${target.email}: $reason",
			sourceIp = sourceIp,
		)
	}

	fun listOrgs(
		q: String?,
		state: String?,
		cursor: AdminCursor?,
		limit: Int,
	): AdminOrgListResponse {
		val normalizedState = state?.trim()?.takeIf { it.isNotEmpty() }?.let {
			if (it in ORG_STATES) it else throw invalidParameter(
				"The parameter 'state' must be one of ${ORG_STATES.joinToString()}.",
			)
		}
		val (items, nextCursor) = query.searchOrgs(
			q = q,
			state = normalizedState,
			cursor = cursor,
			limit = limit,
		)
		return AdminOrgListResponse(items = items, nextCursor = nextCursor)
	}

	@Transactional
	fun getOrg(adminUserId: UUID, orgId: UUID, sourceIp: String?): AdminOrgDetail {
		val detail = query.findOrgDetail(orgId) ?: throw orgNotFound()
		audit.recordPlatform(
			adminUserId = adminUserId,
			action = "org.view",
			targetType = "org",
			targetId = orgId.toString(),
			summary = "Viewed org ${detail.name}",
			sourceIp = sourceIp,
		)
		return detail
	}

	fun listAudit(
		from: Instant?,
		to: Instant?,
		adminUserId: UUID?,
		action: String?,
		cursor: AdminCursor?,
		limit: Int,
	): AdminAuditListResponse {
		if (from != null && to != null && from.isAfter(to)) {
			throw invalidParameter("The parameter 'from' must not be after 'to'.")
		}
		val (items, nextCursor) = query.searchAudit(
			from = from,
			to = to,
			adminUserId = adminUserId,
			action = action?.trim()?.takeIf { it.isNotEmpty() },
			cursor = cursor,
			limit = limit,
		)
		return AdminAuditListResponse(items = items, nextCursor = nextCursor)
	}

	private fun findTarget(userId: UUID): User =
		users.findById(userId).orElseThrow { userNotFound() }

	private fun userNotFound() = ApiException(
		status = HttpStatus.NOT_FOUND,
		code = "NOT_FOUND",
		message = "The requested user does not exist.",
	)

	private fun orgNotFound() = ApiException(
		status = HttpStatus.NOT_FOUND,
		code = "NOT_FOUND",
		message = "The requested organisation does not exist.",
	)

	private fun invalidParameter(message: String) = ApiException(
		status = HttpStatus.BAD_REQUEST,
		code = "INVALID_PARAMETER",
		message = message,
	)

	companion object {
		private val ORG_STATES = setOf("CREATING", "STARTING", "ONLINE", "OFFLINE", "DELETING")
	}
}
