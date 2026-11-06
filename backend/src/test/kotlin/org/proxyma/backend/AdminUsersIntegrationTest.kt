package org.proxyma.backend

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Test
import org.proxyma.backend.user.UserStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

class AdminUsersIntegrationTest : AdminIntegrationTestBase() {

	@Test
	fun `stats count users and orgs including non-online states as offline`() {
		val (adminId, token) = adminSession()
		val activeId = createUser(uniqueEmail("active"))
		val suspendedId = createUser(uniqueEmail("suspended"))
		suspendDirectly(suspendedId)

		createOrg("Online org", adminId, "ONLINE")
		createOrg("Offline org", activeId, "OFFLINE")
		createOrg("Starting org", suspendedId, "STARTING")

		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.users.total").value(3))
			.andExpect(jsonPath("$.users.suspended").value(1))
			.andExpect(jsonPath("$.orgs.total").value(3))
			.andExpect(jsonPath("$.orgs.online").value(1))
			.andExpect(jsonPath("$.orgs.offline").value(2))
	}

	@Test
	fun `user search matches email and display name and exposes org counts`() {
		val (_, token) = adminSession()
		val aliceEmail = uniqueEmail("alice")
		val aliceId = createUser(aliceEmail, "Alice Adams")
		val bobId = createUser(uniqueEmail("bob"), "Bob Brown")
		val orgId = createOrg("Alice's org", aliceId)
		addMembership(orgId, bobId, "member")

		mockMvc.perform(
			get("/api/v1/admin/users")
				.param("q", "alice")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].email").value(aliceEmail))
			.andExpect(jsonPath("$.items[0].owns_org").value(true))
			.andExpect(jsonPath("$.items[0].membership_count").value(1))

		// "adams" appears only in the display name, not in the email.
		mockMvc.perform(
			get("/api/v1/admin/users")
				.param("q", "adams")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(aliceId.toString()))

		mockMvc.perform(
			get("/api/v1/admin/users")
				.param("q", "bob")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(bobId.toString()))
			.andExpect(jsonPath("$.items[0].owns_org").value(false))
			.andExpect(jsonPath("$.items[0].membership_count").value(1))
	}

	@Test
	fun `user search filters by status`() {
		val (_, token) = adminSession()
		val activeId = createUser(uniqueEmail("active"))
		val suspendedId = createUser(uniqueEmail("suspended"))
		suspendDirectly(suspendedId)

		mockMvc.perform(
			get("/api/v1/admin/users")
				.param("status", "suspended")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(suspendedId.toString()))

		mockMvc.perform(
			get("/api/v1/admin/users")
				.param("status", "active")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].status").value("active"))
	}

	@Test
	fun `user search pages with an opaque cursor without duplicates`() {
		val (_, token) = adminSession()
		createUser(uniqueEmail("one"), "One")
		createUser(uniqueEmail("two"), "Two")
		createUser(uniqueEmail("three"), "Three")

		val firstPage = mockMvc.perform(
			get("/api/v1/admin/users")
				.param("limit", "2")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.next_cursor").isNotEmpty)
			.andReturn()

		val cursor = JsonPath.read<String>(firstPage.response.contentAsString, "$.next_cursor")
		val firstIds = JsonPath.read<List<String>>(firstPage.response.contentAsString, "$.items[*].id")

		val secondPage = mockMvc.perform(
			get("/api/v1/admin/users")
				.param("limit", "2")
				.param("cursor", cursor)
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(2))
			.andReturn()

		val secondIds = JsonPath.read<List<String>>(secondPage.response.contentAsString, "$.items[*].id")
		org.junit.jupiter.api.Assertions.assertTrue(
			firstIds.intersect(secondIds.toSet()).isEmpty(),
			"pages must not overlap",
		)
	}

	@Test
	fun `user detail returns metadata only and records a view in the platform audit`() {
		val (adminId, token) = adminSession()
		val email = uniqueEmail("detail")
		val userId = createUser(email, "Detail User")
		val orgId = createOrg("Detail org", userId)
		addMembership(orgId, adminId, "member")
		issueRefreshToken(userId)

		mockMvc.perform(
			get("/api/v1/admin/users/$userId")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(userId.toString()))
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.status").value("active"))
			.andExpect(jsonPath("$.is_platform_admin").value(false))
			.andExpect(jsonPath("$.suspension").isEmpty)
			.andExpect(jsonPath("$.owned_org.id").value(orgId.toString()))
			.andExpect(jsonPath("$.memberships.length()").value(1))
			.andExpect(jsonPath("$.active_sessions").value(1))
			.andExpect(jsonPath("$.password_hash").doesNotExist())
			.andExpect(jsonPath("$.token_hash").doesNotExist())

		val viewEntries = jdbc.queryForObject(
			"SELECT count(*) FROM platform_audit WHERE action = 'user.view' AND target_id = ?",
			Long::class.java,
			userId.toString(),
		)
		org.junit.jupiter.api.Assertions.assertEquals(1L, viewEntries)
	}

	@Test
	fun `suspend rejects a wrong password without changing the account`() {
		val (_, token) = adminSession()
		val userId = createUser(uniqueEmail("target"))

		mockMvc.perform(
			post("/api/v1/admin/users/$userId/suspend")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"Abuse","password":"wrong password"}"""),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.code").value("REAUTH_FAILED"))

		org.junit.jupiter.api.Assertions.assertEquals(
			UserStatus.ACTIVE,
			users.findById(userId).orElseThrow().status,
		)
	}

	@Test
	fun `suspend rejects self and other platform admins`() {
		val (adminId, token) = adminSession()
		val otherAdminEmail = uniqueEmail("other-admin")
		val otherAdminId = createUser(otherAdminEmail, platformAdmin = true)

		mockMvc.perform(
			post("/api/v1/admin/users/$adminId/suspend")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"Self","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value("CANNOT_SUSPEND_SELF"))

		mockMvc.perform(
			post("/api/v1/admin/users/$otherAdminId/suspend")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"Other admin","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.code").value("ADMIN_CANNOT_TARGET_ADMIN"))
	}

	@Test
	fun `suspend requires a reason`() {
		val (_, token) = adminSession()
		val userId = createUser(uniqueEmail("target"))

		mockMvc.perform(
			post("/api/v1/admin/users/$userId/suspend")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"   ","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().`is`(422))
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.errors[0].path").value("/reason"))
	}

	@Test
	fun `suspend revokes sessions and records platform and org audit entries`() {
		val (adminId, token) = adminSession()
		val email = uniqueEmail("target")
		val userId = createUser(email, "Target User")
		val orgId = createOrg("Target org", userId)
		issueRefreshToken(userId)
		val targetToken = accessToken(login(email))
		val tokenVersionBefore = users.findById(userId).orElseThrow().tokenVersion

		mockMvc.perform(
			post("/api/v1/admin/users/$userId/suspend")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"Repeated abuse","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("suspended"))
			.andExpect(jsonPath("$.suspension.reason").value("Repeated abuse"))
			.andExpect(jsonPath("$.suspension.suspended_by.user_id").value(adminId.toString()))

		val target = users.findById(userId).orElseThrow()
		org.junit.jupiter.api.Assertions.assertEquals(UserStatus.SUSPENDED, target.status)
		org.junit.jupiter.api.Assertions.assertTrue(target.tokenVersion > tokenVersionBefore)
		org.junit.jupiter.api.Assertions.assertEquals(
			0L,
			jdbc.queryForObject(
				"SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
				Long::class.java,
				userId,
			),
		)

		// Suspending a user does not stop their org or proxy.
		org.junit.jupiter.api.Assertions.assertEquals(
			"ONLINE",
			jdbc.queryForObject(
				"SELECT state FROM organisations WHERE id = ?",
				String::class.java,
				orgId,
			),
		)
		org.junit.jupiter.api.Assertions.assertEquals(
			1L,
			jdbc.queryForObject(
				"SELECT count(*) FROM org_memberships WHERE org_id = ? AND user_id = ?",
				Long::class.java,
				orgId,
				userId,
			),
		)

		// A suspended account is rejected before the token-version check.
		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer $targetToken"))
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))

		val platformEntries = jdbc.queryForObject(
			"SELECT count(*) FROM platform_audit WHERE action = 'user.suspend' AND target_id = ?",
			Long::class.java,
			userId.toString(),
		)
		org.junit.jupiter.api.Assertions.assertEquals(1L, platformEntries)

		val orgEntries = jdbc.queryForObject(
			"""
			SELECT count(*) FROM org_audit_entries
			WHERE org_id = ? AND action = 'member.suspend' AND actor_type = 'platform_admin'
			""".trimIndent(),
			Long::class.java,
			orgId,
		)
		org.junit.jupiter.api.Assertions.assertEquals(1L, orgEntries)
	}

	@Test
	fun `unsuspend restores the account and records both audit trails`() {
		val (_, token) = adminSession()
		val userId = createUser(uniqueEmail("target"), "Target User")
		val orgId = createOrg("Target org", userId)
		suspendDirectly(userId)

		mockMvc.perform(
			post("/api/v1/admin/users/$userId/unsuspend")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"Resolved after contact"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("active"))
			.andExpect(jsonPath("$.suspension").isEmpty)

		org.junit.jupiter.api.Assertions.assertEquals(
			UserStatus.ACTIVE,
			users.findById(userId).orElseThrow().status,
		)
		org.junit.jupiter.api.Assertions.assertEquals(
			1L,
			jdbc.queryForObject(
				"SELECT count(*) FROM platform_audit WHERE action = 'user.unsuspend' AND target_id = ?",
				Long::class.java,
				userId.toString(),
			),
		)
		org.junit.jupiter.api.Assertions.assertEquals(
			1L,
			jdbc.queryForObject(
				"""
				SELECT count(*) FROM org_audit_entries
				WHERE org_id = ? AND action = 'member.unsuspend' AND actor_type = 'platform_admin'
				""".trimIndent(),
				Long::class.java,
				orgId,
			),
		)
	}

	@Test
	fun `force logout revokes sessions and records the action`() {
		val (_, token) = adminSession()
		val email = uniqueEmail("target")
		val userId = createUser(email, "Target User")
		issueRefreshToken(userId)
		val targetToken = accessToken(login(email))

		mockMvc.perform(
			post("/api/v1/admin/users/$userId/force-logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"Possible compromise","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer $targetToken"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

		org.junit.jupiter.api.Assertions.assertEquals(
			0L,
			jdbc.queryForObject(
				"SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
				Long::class.java,
				userId,
			),
		)
		org.junit.jupiter.api.Assertions.assertEquals(
			1L,
			jdbc.queryForObject(
				"SELECT count(*) FROM platform_audit WHERE action = 'user.force_logout' AND target_id = ?",
				Long::class.java,
				userId.toString(),
			),
		)
	}

	@Test
	fun `force logout can target another platform admin`() {
		val (_, token) = adminSession()
		val otherAdminId = createUser(uniqueEmail("other-admin"), platformAdmin = true)

		mockMvc.perform(
			post("/api/v1/admin/users/$otherAdminId/force-logout")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"reason":"Session cleanup","password":"$VALID_PASSWORD"}"""),
		)
			.andExpect(status().isNoContent)
	}

	private fun adminSession(): Pair<UUID, String> {
		val email = uniqueEmail("admin")
		val userId = createUser(email, "Platform Admin", platformAdmin = true)
		return userId to accessToken(login(email))
	}

	private fun suspendDirectly(userId: UUID) {
		val user = users.findById(userId).orElseThrow()
		user.status = UserStatus.SUSPENDED
		user.suspendedAt = Instant.now()
		user.suspensionReason = "test"
		users.save(user)
	}

	private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"
}
