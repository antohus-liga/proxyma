package org.proxyma.backend.admin

import org.proxyma.backend.admin.dto.AdminAuditEntry
import org.proxyma.backend.admin.dto.AdminAuditTarget
import org.proxyma.backend.admin.dto.AdminMembership
import org.proxyma.backend.admin.dto.AdminOrgDetail
import org.proxyma.backend.admin.dto.AdminOrgMember
import org.proxyma.backend.admin.dto.AdminOrgProxy
import org.proxyma.backend.admin.dto.AdminOrgRef
import org.proxyma.backend.admin.dto.AdminOrgStats
import org.proxyma.backend.admin.dto.AdminOrgSummary
import org.proxyma.backend.admin.dto.AdminStatsResponse
import org.proxyma.backend.admin.dto.AdminSuspension
import org.proxyma.backend.admin.dto.AdminUserDetail
import org.proxyma.backend.admin.dto.AdminUserRef
import org.proxyma.backend.admin.dto.AdminUserStats
import org.proxyma.backend.admin.dto.AdminUserSummary
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Read model for the admin API.
 *
 * It intentionally uses SQL directly instead of mapping the whole org/config/
 * ban domain as JPA entities. The admin API only needs metadata, and keeping
 * those queries here makes it impossible to accidentally serialise a config
 * document, a log row or a ban IP.
 */
@Repository
class AdminQueryRepository(
	private val jdbc: NamedParameterJdbcTemplate,
) {

	fun stats(): AdminStatsResponse {
		val users = jdbc.query(
			"""
			SELECT count(*) AS total,
			       count(*) FILTER (WHERE status = 'suspended') AS suspended
			FROM users
			""".trimIndent(),
		) { rs, _ ->
			AdminUserStats(
				total = rs.getLong("total"),
				suspended = rs.getLong("suspended"),
			)
		}.single()

		val orgs = jdbc.query(
			"""
			SELECT count(*) AS total,
			       count(*) FILTER (WHERE state = 'ONLINE') AS online
			FROM organisations
			""".trimIndent(),
		) { rs, _ ->
			val total = rs.getLong("total")
			val online = rs.getLong("online")
			AdminOrgStats(
				total = total,
				online = online,
				offline = total - online,
			)
		}.single()

		return AdminStatsResponse(users = users, orgs = orgs)
	}

	fun searchUsers(
		q: String?,
		status: String?,
		cursor: AdminCursor?,
		limit: Int,
	): Pair<List<AdminUserSummary>, String?> {
		val conditions = mutableListOf<String>()
		val params = MapSqlParameterSource()

		if (!q.isNullOrBlank()) {
			conditions += """
				(strpos(lower(u.email::text), lower(:q)) > 0
				 OR strpos(lower(u.display_name), lower(:q)) > 0)
			""".trimIndent()
			params.addValue("q", q.trim())
		}
		if (!status.isNullOrBlank()) {
			conditions += "u.status = :status"
			params.addValue("status", status)
		}
		if (cursor != null) {
			conditions += """
				(u.created_at, u.id) <
				(CAST(:cursorCreatedAt AS timestamptz), CAST(:cursorId AS uuid))
			""".trimIndent()
			params.addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt))
			params.addValue("cursorId", cursor.id)
		}

		val where = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
		params.addValue("limit", limit + 1)

		val rows = jdbc.query(
			"""
			SELECT u.id, u.email, u.display_name, u.status, u.created_at, u.last_login_at,
			       EXISTS (
			           SELECT 1 FROM org_memberships m
			           WHERE m.user_id = u.id AND m.role = 'owner'
			       ) AS owns_org,
			       (
			           SELECT count(*) FROM org_memberships m WHERE m.user_id = u.id
			       ) AS membership_count
			FROM users u
			$where
			ORDER BY u.created_at DESC, u.id DESC
			LIMIT :limit
			""".trimIndent(),
			params,
		) { rs, _ ->
			AdminUserSummary(
				id = rs.getObject("id", UUID::class.java),
				email = rs.getString("email"),
				displayName = rs.getString("display_name"),
				status = rs.getString("status"),
				createdAt = rs.instant("created_at"),
				lastLoginAt = rs.instantOrNull("last_login_at"),
				ownsOrg = rs.getBoolean("owns_org"),
				membershipCount = rs.getLong("membership_count"),
			)
		}

		return rows.page(limit)
	}

	fun findUserDetail(id: UUID): AdminUserDetail? {
		val user = jdbc.query(
			"""
			SELECT u.id, u.email, u.display_name, u.status, u.is_platform_admin,
			       u.created_at, u.last_login_at, u.suspension_reason, u.suspended_at,
			       s.id AS suspended_by_id,
			       s.display_name AS suspended_by_display_name
			FROM users u
			LEFT JOIN users s ON s.id = u.suspended_by_user_id
			WHERE u.id = :id
			""".trimIndent(),
			mapOf("id" to id),
		) { rs, _ ->
			val suspendedAt = rs.instantOrNull("suspended_at")
			val suspendedById = rs.getObject("suspended_by_id", UUID::class.java)
			AdminUserDetail(
				id = rs.getObject("id", UUID::class.java),
				email = rs.getString("email"),
				displayName = rs.getString("display_name"),
				status = rs.getString("status"),
				suspension = suspendedAt?.let {
					AdminSuspension(
						reason = rs.getString("suspension_reason"),
						suspendedAt = it,
						suspendedBy = suspendedById?.let { userId ->
							AdminUserRef(
								userId = userId,
								displayName = rs.getString("suspended_by_display_name"),
							)
						},
					)
				},
				platformAdmin = rs.getBoolean("is_platform_admin"),
				createdAt = rs.instant("created_at"),
				lastLoginAt = rs.instantOrNull("last_login_at"),
				ownedOrg = null,
				memberships = emptyList(),
				activeSessions = 0,
			)
		}.singleOrNull() ?: return null

		val ownedOrg = jdbc.query(
			"""
			SELECT o.id, o.name
			FROM org_memberships m
			JOIN organisations o ON o.id = m.org_id
			WHERE m.user_id = :id AND m.role = 'owner'
			""".trimIndent(),
			mapOf("id" to id),
		) { rs, _ ->
			AdminOrgRef(
				id = rs.getObject("id", UUID::class.java),
				name = rs.getString("name"),
			)
		}.singleOrNull()

		val memberships = jdbc.query(
			"""
			SELECT m.org_id, o.name AS org_name, m.role, m.joined_at
			FROM org_memberships m
			JOIN organisations o ON o.id = m.org_id
			WHERE m.user_id = :id
			ORDER BY m.joined_at, o.name
			""".trimIndent(),
			mapOf("id" to id),
		) { rs, _ ->
			AdminMembership(
				orgId = rs.getObject("org_id", UUID::class.java),
				orgName = rs.getString("org_name"),
				role = rs.getString("role"),
				joinedAt = rs.instant("joined_at"),
			)
		}

		val activeSessions = jdbc.queryForObject(
			"""
			SELECT count(*)
			FROM refresh_tokens
			WHERE user_id = :id AND revoked_at IS NULL AND expires_at > now()
			""".trimIndent(),
			mapOf("id" to id),
			Long::class.java,
		) ?: 0L

		return user.copy(
			ownedOrg = ownedOrg,
			memberships = memberships,
			activeSessions = activeSessions,
		)
	}

	fun orgIdsForUser(userId: UUID): List<UUID> =
		jdbc.query(
			"""
			SELECT org_id
			FROM org_memberships
			WHERE user_id = :userId
			ORDER BY org_id
			""".trimIndent(),
			mapOf("userId" to userId),
		) { rs, _ -> rs.getObject("org_id", UUID::class.java) }

	fun searchOrgs(
		q: String?,
		state: String?,
		cursor: AdminCursor?,
		limit: Int,
	): Pair<List<AdminOrgSummary>, String?> {
		val conditions = mutableListOf<String>()
		val params = MapSqlParameterSource()

		if (!q.isNullOrBlank()) {
			conditions += """
				(strpos(lower(o.name), lower(:q)) > 0
				 OR strpos(lower(owner_user.display_name), lower(:q)) > 0
				 OR strpos(lower(owner_user.email::text), lower(:q)) > 0)
			""".trimIndent()
			params.addValue("q", q.trim())
		}
		if (!state.isNullOrBlank()) {
			conditions += "o.state = :state"
			params.addValue("state", state)
		}
		if (cursor != null) {
			conditions += """
				(o.created_at, o.id) <
				(CAST(:cursorCreatedAt AS timestamptz), CAST(:cursorId AS uuid))
			""".trimIndent()
			params.addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt))
			params.addValue("cursorId", cursor.id)
		}

		val where = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
		params.addValue("limit", limit + 1)

		val rows = jdbc.query(
			"""
			SELECT o.id, o.name, o.state, o.created_at,
			       owner_user.id AS owner_user_id,
			       owner_user.display_name AS owner_display_name,
			       (
			           SELECT count(*) FROM org_memberships m WHERE m.org_id = o.id
			       ) AS member_count
			FROM organisations o
			LEFT JOIN org_memberships owner_membership
			       ON owner_membership.org_id = o.id AND owner_membership.role = 'owner'
			LEFT JOIN users owner_user ON owner_user.id = owner_membership.user_id
			$where
			ORDER BY o.created_at DESC, o.id DESC
			LIMIT :limit
			""".trimIndent(),
			params,
		) { rs, _ ->
			val ownerId = rs.getObject("owner_user_id", UUID::class.java)
			AdminOrgSummary(
				id = rs.getObject("id", UUID::class.java),
				name = rs.getString("name"),
				state = rs.getString("state"),
				owner = ownerId?.let {
					AdminUserRef(
						userId = it,
						displayName = rs.getString("owner_display_name"),
					)
				},
				memberCount = rs.getLong("member_count"),
				createdAt = rs.instant("created_at"),
			)
		}

		return rows.page(limit)
	}

	fun findOrgDetail(id: UUID): AdminOrgDetail? {
		val org = jdbc.query(
			"""
			SELECT o.id, o.name, o.state, o.created_at, o.last_heartbeat_at,
			       o.running_config_version,
			       owner_user.id AS owner_user_id,
			       owner_user.display_name AS owner_display_name
			FROM organisations o
			LEFT JOIN org_memberships owner_membership
			       ON owner_membership.org_id = o.id AND owner_membership.role = 'owner'
			LEFT JOIN users owner_user ON owner_user.id = owner_membership.user_id
			WHERE o.id = :id
			""".trimIndent(),
			mapOf("id" to id),
		) { rs, _ ->
			val ownerId = rs.getObject("owner_user_id", UUID::class.java)
			AdminOrgDetail(
				id = rs.getObject("id", UUID::class.java),
				name = rs.getString("name"),
				state = rs.getString("state"),
				createdAt = rs.instant("created_at"),
				owner = ownerId?.let {
					AdminUserRef(
						userId = it,
						displayName = rs.getString("owner_display_name"),
					)
				},
				members = emptyList(),
				proxy = AdminOrgProxy(
					lastHeartbeatAt = rs.instantOrNull("last_heartbeat_at"),
					runningConfigVersion = rs.getIntOrNull("running_config_version"),
					latestConfigVersion = null,
					configStatus = "pending",
					activeBanCount = 0,
				),
			)
		}.singleOrNull() ?: return null

		val members = jdbc.query(
			"""
			SELECT m.user_id, u.display_name, m.role
			FROM org_memberships m
			JOIN users u ON u.id = m.user_id
			WHERE m.org_id = :id
			ORDER BY CASE m.role
			             WHEN 'owner' THEN 0
			             WHEN 'co_owner' THEN 1
			             ELSE 2
			         END,
			         u.display_name
			""".trimIndent(),
			mapOf("id" to id),
		) { rs, _ ->
			AdminOrgMember(
				userId = rs.getObject("user_id", UUID::class.java),
				displayName = rs.getString("display_name"),
				role = rs.getString("role"),
			)
		}

		val configState = jdbc.query(
			"""
			SELECT latest_version, applied_version
			FROM org_config_state
			WHERE org_id = :id
			""".trimIndent(),
			mapOf("id" to id),
		) { rs, _ ->
			ConfigState(
				latestVersion = rs.getIntOrNull("latest_version"),
				appliedVersion = rs.getIntOrNull("applied_version"),
			)
		}.singleOrNull()

		val latestRejected = configState?.latestVersion?.let { version ->
			jdbc.queryForObject(
				"""
				SELECT rejected_at IS NOT NULL
				FROM org_configs
				WHERE org_id = :id AND version = :version
				""".trimIndent(),
				mapOf("id" to id, "version" to version),
				Boolean::class.java,
			) ?: false
		} ?: false

		val activeBanCount = jdbc.queryForObject(
			"""
			SELECT count(*)
			FROM bans
			WHERE org_id = :id
			  AND removed_at IS NULL
			  AND (expires_at IS NULL OR expires_at > now())
			""".trimIndent(),
			mapOf("id" to id),
			Long::class.java,
		) ?: 0L

		val configStatus = when {
			configState == null -> "pending"
			configState.appliedVersion != null &&
				configState.appliedVersion == configState.latestVersion -> "applied"
			latestRejected -> "rejected"
			else -> "pending"
		}

		return org.copy(
			members = members,
			proxy = org.proxy.copy(
				latestConfigVersion = configState?.latestVersion,
				configStatus = configStatus,
				activeBanCount = activeBanCount,
			),
		)
	}

	fun searchAudit(
		from: Instant?,
		to: Instant?,
		adminUserId: UUID?,
		action: String?,
		cursor: AdminCursor?,
		limit: Int,
	): Pair<List<AdminAuditEntry>, String?> {
		val conditions = mutableListOf<String>()
		val params = MapSqlParameterSource()

		if (from != null) {
			conditions += "pa.created_at >= :from"
			params.addValue("from", Timestamp.from(from))
		}
		if (to != null) {
			conditions += "pa.created_at < :to"
			params.addValue("to", Timestamp.from(to))
		}
		if (adminUserId != null) {
			conditions += "pa.admin_user_id = :adminUserId"
			params.addValue("adminUserId", adminUserId)
		}
		if (!action.isNullOrBlank()) {
			conditions += "pa.action = :action"
			params.addValue("action", action)
		}
		if (cursor != null) {
			conditions += """
				(pa.created_at, pa.id) <
				(CAST(:cursorCreatedAt AS timestamptz), CAST(:cursorId AS uuid))
			""".trimIndent()
			params.addValue("cursorCreatedAt", Timestamp.from(cursor.createdAt))
			params.addValue("cursorId", cursor.id)
		}

		val where = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
		params.addValue("limit", limit + 1)

		val rows = jdbc.query(
			"""
			SELECT pa.id, pa.created_at, pa.action, pa.target_type, pa.target_id,
			       pa.summary, host(pa.source_ip) AS source_ip,
			       u.id AS admin_id, u.display_name AS admin_display_name
			FROM platform_audit pa
			LEFT JOIN users u ON u.id = pa.admin_user_id
			$where
			ORDER BY pa.created_at DESC, pa.id DESC
			LIMIT :limit
			""".trimIndent(),
			params,
		) { rs, _ ->
			val adminId = rs.getObject("admin_id", UUID::class.java)
			val targetType = rs.getString("target_type")
			val targetId = rs.getString("target_id")
			AdminAuditEntry(
				id = rs.getObject("id", UUID::class.java),
				timestamp = rs.instant("created_at"),
				admin = adminId?.let {
					AdminUserRef(
						userId = it,
						displayName = rs.getString("admin_display_name"),
					)
				},
				action = rs.getString("action"),
				target = if (targetType == null && targetId == null) {
					null
				} else {
					AdminAuditTarget(type = targetType, id = targetId)
				},
				summary = rs.getString("summary"),
				sourceIp = rs.getString("source_ip"),
			)
		}

		return rows.page(limit)
	}

	private data class ConfigState(
		val latestVersion: Int?,
		val appliedVersion: Int?,
	)

	private fun <T> List<T>.page(limit: Int): Pair<List<T>, String?> {
		if (size <= limit) {
			return this to null
		}
		val page = take(limit)
        val cursor = when (val last = page.last()) {
			is AdminUserSummary -> AdminCursorCodec.encode(last.createdAt, last.id)
			is AdminOrgSummary -> AdminCursorCodec.encode(last.createdAt, last.id)
			is AdminAuditEntry -> AdminCursorCodec.encode(last.timestamp, last.id)
			else -> error("Unsupported cursor row: $last")
		}
		return page to cursor
	}

	private fun ResultSet.instant(column: String): Instant =
		getTimestamp(column).toInstant()

	private fun ResultSet.instantOrNull(column: String): Instant? =
		getTimestamp(column)?.toInstant()

	private fun ResultSet.getIntOrNull(column: String): Int? {
		val value = getInt(column)
		return if (wasNull()) null else value
	}
}
