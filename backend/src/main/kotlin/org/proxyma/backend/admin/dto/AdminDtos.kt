package org.proxyma.backend.admin.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// ---------------------------------------------------------------------------
// Platform stats
// ---------------------------------------------------------------------------

data class AdminStatsResponse(
	val users: AdminUserStats,
	val orgs: AdminOrgStats,
)

data class AdminUserStats(
	val total: Long,
	val suspended: Long,
)

data class AdminOrgStats(
	val total: Long,
	val online: Long,
	val offline: Long,
)

// ---------------------------------------------------------------------------
// Users
// ---------------------------------------------------------------------------

data class AdminUserSummary(
	val id: UUID,
	val email: String,
	val displayName: String,
	val status: String,
	val createdAt: Instant,
	val lastLoginAt: Instant?,
	val ownsOrg: Boolean,
	val membershipCount: Long,
)

data class AdminUserListResponse(
	val items: List<AdminUserSummary>,
	val nextCursor: String?,
)

data class AdminUserDetail(
	val id: UUID,
	val email: String,
	val displayName: String,
	val status: String,
	val suspension: AdminSuspension?,
	@get:JsonProperty("is_platform_admin")
	@param:JsonProperty("is_platform_admin")
	val platformAdmin: Boolean,
	val createdAt: Instant,
	val lastLoginAt: Instant?,
	val ownedOrg: AdminOrgRef?,
	val memberships: List<AdminMembership>,
	val activeSessions: Long,
)

data class AdminSuspension(
	val reason: String?,
	val suspendedAt: Instant,
	val suspendedBy: AdminUserRef?,
)

data class AdminUserRef(
	val userId: UUID,
	val displayName: String,
)

data class AdminOrgRef(
	val id: UUID,
	val name: String,
)

data class AdminMembership(
	val orgId: UUID,
	val orgName: String,
	val role: String,
	val joinedAt: Instant,
)

// ---------------------------------------------------------------------------
// Organisations
// ---------------------------------------------------------------------------

data class AdminOrgSummary(
	val id: UUID,
	val name: String,
	val state: String,
	val owner: AdminUserRef?,
	val memberCount: Long,
	val createdAt: Instant,
)

data class AdminOrgListResponse(
	val items: List<AdminOrgSummary>,
	val nextCursor: String?,
)

data class AdminOrgDetail(
	val id: UUID,
	val name: String,
	val state: String,
	val createdAt: Instant,
	val owner: AdminUserRef?,
	val members: List<AdminOrgMember>,
	val proxy: AdminOrgProxy,
)

data class AdminOrgMember(
	val userId: UUID,
	val displayName: String,
	val role: String,
)

data class AdminOrgProxy(
	val lastHeartbeatAt: Instant?,
	val runningConfigVersion: Int?,
	val latestConfigVersion: Int?,
	val configStatus: String,
	val activeBanCount: Long,
)

// ---------------------------------------------------------------------------
// Platform audit
// ---------------------------------------------------------------------------

data class AdminAuditEntry(
	val id: UUID,
	val timestamp: Instant,
	val admin: AdminUserRef?,
	val action: String,
	val target: AdminAuditTarget?,
	val summary: String,
	val sourceIp: String?,
)

data class AdminAuditTarget(
	val type: String?,
	val id: String?,
)

data class AdminAuditListResponse(
	val items: List<AdminAuditEntry>,
	val nextCursor: String?,
)

// ---------------------------------------------------------------------------
// Requests
// ---------------------------------------------------------------------------

data class AdminSuspendRequest(
	@field:NotBlank(message = "must not be blank")
	@field:Size(max = 500, message = "must be at most 500 characters")
	val reason: String,

	@field:NotBlank(message = "must not be blank")
	val password: String,
)

data class AdminUnsuspendRequest(
	@field:NotBlank(message = "must not be blank")
	@field:Size(max = 500, message = "must be at most 500 characters")
	val reason: String,
)

data class AdminForceLogoutRequest(
	@field:NotBlank(message = "must not be blank")
	@field:Size(max = 500, message = "must be at most 500 characters")
	val reason: String,

	@field:NotBlank(message = "must not be blank")
	val password: String,
)
