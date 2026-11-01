package org.proxyma.backend

import com.jayway.jsonpath.JsonPath
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.proxyma.backend.admin.PlatformAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class AdminAuditIntegrationTest : AdminIntegrationTestBase() {

	@Autowired
	private lateinit var platformAdmins: PlatformAdminService

	@Test
	fun `admin actions and views are recorded newest first with actor, target and source`() {
		val (adminId, token) = adminSession()
		val targetId = createUser(uniqueEmail("target"), "Target User")
		val orgId = createOrg("Target org", targetId)

		mockMvc.perform(
			get("/api/v1/admin/users/$targetId")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		).andExpect(status().isOk)
		mockMvc.perform(
			get("/api/v1/admin/orgs/$orgId")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		).andExpect(status().isOk)
		mockMvc.perform(
			get("/api/v1/admin/users/$targetId")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		).andExpect(status().isOk)

		val result = mockMvc.perform(
			get("/api/v1/admin/audit")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(3))
			// The last request was the most recent, so it comes first.
			.andExpect(jsonPath("$.items[0].action").value("user.view"))
			.andExpect(jsonPath("$.items[0].admin.user_id").value(adminId.toString()))
			.andExpect(jsonPath("$.items[0].target.type").value("user"))
			.andExpect(jsonPath("$.items[0].target.id").value(targetId.toString()))
			.andExpect(jsonPath("$.items[0].source_ip").value("127.0.0.1"))
			.andReturn()

		val body = result.response.contentAsString
		val actions = JsonPath.read<List<String>>(body, "$.items[*].action")
		assertEquals(listOf("org.view", "user.view", "user.view"), actions.sorted())
		val orgTargets = JsonPath.read<List<String>>(body, "$.items[?(@.action == 'org.view')].target.id")
		assertEquals(listOf(orgId.toString()), orgTargets)
	}

	@Test
	fun `audit filters by action and by admin`() {
		val (adminId, token) = adminSession()
		val otherAdminId = createUser(uniqueEmail("other-admin"), platformAdmin = true)
		insertAudit(adminId, "user.view", "user", UUID.randomUUID(), "2026-01-01T00:00:00Z")
		insertAudit(adminId, "user.suspend", "user", UUID.randomUUID(), "2026-01-01T00:00:01Z")
		insertAudit(otherAdminId, "user.view", "user", UUID.randomUUID(), "2026-01-01T00:00:02Z")

		mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("action", "user.suspend")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].action").value("user.suspend"))

		mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("admin_user_id", otherAdminId.toString())
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].admin.user_id").value(otherAdminId.toString()))
	}

	@Test
	fun `audit filters by time range with an exclusive upper bound`() {
		val (adminId, token) = adminSession()
		insertAudit(adminId, "user.view", "user", UUID.randomUUID(), "2026-01-01T00:00:00Z")
		insertAudit(adminId, "user.view", "user", UUID.randomUUID(), "2026-01-02T00:00:00Z")
		insertAudit(adminId, "user.view", "user", UUID.randomUUID(), "2026-01-03T00:00:00Z")

		mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("from", "2026-01-02T00:00:00Z")
				.param("to", "2026-01-03T00:00:00Z")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].timestamp").value("2026-01-02T00:00:00Z"))
	}

	@Test
	fun `audit pages with an opaque cursor without duplicates`() {
		val (adminId, token) = adminSession()
		insertAudit(adminId, "user.view", "user", UUID.randomUUID(), "2026-01-01T00:00:00Z")
		insertAudit(adminId, "user.view", "user", UUID.randomUUID(), "2026-01-02T00:00:00Z")
		insertAudit(adminId, "user.view", "user", UUID.randomUUID(), "2026-01-03T00:00:00Z")

		val first = mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("limit", "2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.next_cursor").isNotEmpty)
			.andReturn()

		val cursor = JsonPath.read<String>(first.response.contentAsString, "$.next_cursor")
		val firstIds = JsonPath.read<List<String>>(first.response.contentAsString, "$.items[*].id")

		val second = mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("limit", "2")
				.param("cursor", cursor)
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andReturn()

		val secondIds = JsonPath.read<List<String>>(second.response.contentAsString, "$.items[*].id")
		assertEquals(0, firstIds.intersect(secondIds.toSet()).size, "pages must not overlap")
	}

	@Test
	fun `operator command entries have no admin and no source address`() {
		val (_, token) = adminSession()
		val email = uniqueEmail("promoted")
		createUser(email, "Promoted User")

		platformAdmins.grant(email)

		mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("action", "platform_admin.grant")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].admin").value(nullValue()))
			.andExpect(jsonPath("$.items[0].source_ip").value(nullValue()))
			.andExpect(jsonPath("$.items[0].target.type").value("user"))
	}

	@Test
	fun `an org audit entry is not visible through the platform audit endpoint`() {
		val (adminId, token) = adminSession()
		val orgId = createOrg("Org", adminId)
		jdbc.update(
			"""
			INSERT INTO org_audit_entries (
			    org_id, actor_type, actor_user_id, action, summary
			) VALUES (?, 'user', ?, 'member.invite', 'org-only entry')
			""".trimIndent(),
			orgId,
			adminId,
		)

		mockMvc.perform(
			get("/api/v1/admin/audit")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(0))
	}

	@Test
	fun `an invalid time range or cursor is rejected`() {
		val (_, token) = adminSession()

		mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("from", "2026-01-02T00:00:00Z")
				.param("to", "2026-01-01T00:00:00Z")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))

		mockMvc.perform(
			get("/api/v1/admin/audit")
				.param("cursor", "not-a-cursor")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("INVALID_CURSOR"))
	}

	private fun insertAudit(
		adminUserId: UUID?,
		action: String,
		targetType: String,
		targetId: UUID,
		createdAt: String,
	) {
		jdbc.update(
			"""
			INSERT INTO platform_audit (
			    admin_user_id, action, target_type, target_id, summary, source_ip, created_at
			) VALUES (?, ?, ?, ?, 'test entry', CAST('203.0.113.10' AS inet), ?)
			""".trimIndent(),
			adminUserId,
			action,
			targetType,
			targetId.toString(),
			Timestamp.from(Instant.parse(createdAt)),
		)
	}

	private fun adminSession(): Pair<UUID, String> {
		val email = uniqueEmail("admin")
		val userId = createUser(email, "Platform Admin", platformAdmin = true)
		return userId to accessToken(login(email))
	}

	private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"
}
