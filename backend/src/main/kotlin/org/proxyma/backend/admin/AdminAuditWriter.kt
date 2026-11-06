package org.proxyma.backend.admin

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Writes to the platform audit trail and, for actions that affect an org, to
 * that org's own audit trail.
 *
 * Both writes happen inside the caller's transaction, so an action cannot
 * commit without its audit record.
 */
@Repository
class AdminAuditWriter(
	private val jdbc: NamedParameterJdbcTemplate,
) {

	fun recordPlatform(
		adminUserId: UUID?,
		action: String,
		targetType: String?,
		targetId: String?,
		summary: String,
		sourceIp: String?,
	) {
		jdbc.update(
			"""
			INSERT INTO platform_audit (
			    admin_user_id, action, target_type, target_id, summary, source_ip
			) VALUES (
			    :adminUserId, :action, :targetType, :targetId, :summary,
			    CAST(:sourceIp AS inet)
			)
			""".trimIndent(),
			MapSqlParameterSource()
				.addValue("adminUserId", adminUserId)
				.addValue("action", action)
				.addValue("targetType", targetType)
				.addValue("targetId", targetId)
				.addValue("summary", summary)
				.addValue("sourceIp", sourceIp),
		)
	}

	fun recordOrg(
		orgId: UUID,
		adminUserId: UUID,
		action: String,
		targetType: String?,
		targetId: String?,
		summary: String,
		sourceIp: String?,
	) {
		jdbc.update(
			"""
			INSERT INTO org_audit_entries (
			    org_id, actor_type, actor_user_id, action, target_type, target_id,
			    summary, source_ip
			) VALUES (
			    :orgId, 'platform_admin', :adminUserId, :action, :targetType,
			    :targetId, :summary, CAST(:sourceIp AS inet)
			)
			""".trimIndent(),
			MapSqlParameterSource()
				.addValue("orgId", orgId)
				.addValue("adminUserId", adminUserId)
				.addValue("action", action)
				.addValue("targetType", targetType)
				.addValue("targetId", targetId)
				.addValue("summary", summary)
				.addValue("sourceIp", sourceIp),
		)
	}
}
