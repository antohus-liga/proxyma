package org.proxyma.backend

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.junit.jupiter.api.Test
import java.util.UUID

class AdminOrgsIntegrationTest : AdminIntegrationTestBase() {

	@Test
	fun `org search matches the name, the owner name and the owner email`() {
		val (adminId, token) = adminSession()
		val aliceEmail = uniqueEmail("alice")
		val aliceId = createUser(aliceEmail, "Alice Adams")
		val bobId = createUser(uniqueEmail("bob"), "Bob Brown")
		val acmeId = createOrg("Acme Corp", aliceId)
		addMembership(acmeId, bobId, "member")
		val betaId = createOrg("Beta Labs", bobId)

		// The admin is not a member of either org; admin powers are independent
		// of org membership.
		assertFalse(jdbc.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM org_memberships WHERE user_id = ?)",
			Boolean::class.java,
			adminId,
		)!!)

		search(token, "acme")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(acmeId.toString()))
			.andExpect(jsonPath("$.items[0].owner.user_id").value(aliceId.toString()))
			.andExpect(jsonPath("$.items[0].member_count").value(2))

		search(token, "alice")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(acmeId.toString()))

		// Owner email search is case-insensitive too.
		search(token, aliceEmail.uppercase())
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(acmeId.toString()))

		// "Adams" appears only in the owner's display name, not in the org name
		// or the owner email.
		search(token, "Adams")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(acmeId.toString()))

		search(token, "beta")
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(betaId.toString()))
			.andExpect(jsonPath("$.items[0].owner.display_name").value("Bob Brown"))
	}

	@Test
	fun `org search filters by state and rejects an unknown state`() {
		val (_, token) = adminSession()
		val online = createOrg("Online", createUser(uniqueEmail("online-owner")))
		createOrg("Offline", createUser(uniqueEmail("offline-owner")), "OFFLINE")
		createOrg("Starting", createUser(uniqueEmail("starting-owner")), "STARTING")

		mockMvc.perform(
			get("/api/v1/admin/orgs")
				.param("state", "ONLINE")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(online.toString()))

		mockMvc.perform(
			get("/api/v1/admin/orgs")
				.param("state", "RUNNING")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
	}

	@Test
	fun `org search pages with an opaque cursor without duplicates`() {
		val (_, token) = adminSession()
		createOrg("One", createUser(uniqueEmail("one-owner")))
		createOrg("Two", createUser(uniqueEmail("two-owner")))
		createOrg("Three", createUser(uniqueEmail("three-owner")))

		val first = mockMvc.perform(
			get("/api/v1/admin/orgs")
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
			get("/api/v1/admin/orgs")
				.param("limit", "2")
				.param("cursor", cursor)
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andReturn()

		val secondIds = JsonPath.read<List<String>>(second.response.contentAsString, "$.items[*].id")
		assertTrue(firstIds.intersect(secondIds.toSet()).isEmpty(), "pages must not overlap")
	}

	@Test
	fun `org detail exposes members, proxy status and ban count but never config or ban IPs`() {
		val (_, token) = adminSession()
		val ownerId = createUser(uniqueEmail("owner"), "Owner One")
		val memberId = createUser(uniqueEmail("member"), "Member Two")
		val orgId = createOrg("Acme", ownerId)
		addMembership(orgId, memberId, "member")

		jdbc.update(
			"""
			UPDATE organisations
			SET last_heartbeat_at = now() - interval '30 seconds',
			    running_config_version = 2
			WHERE id = ?
			""".trimIndent(),
			orgId,
		)
		createConfig(orgId, version = 2, appliedVersion = 2)
		createBan(orgId, "198.51.100.23")
		// Expired and removed bans are not "active".
		jdbc.update(
			"""
			INSERT INTO bans (org_id, ip, source, reason, created_at, expires_at)
			VALUES (?, CAST(? AS inet), 'auto', 'expired', now() - interval '2 hours', now() - interval '1 hour')
			""".trimIndent(),
			orgId,
			"198.51.100.24",
		)
		jdbc.update(
			"""
			INSERT INTO bans (org_id, ip, source, reason, removed_at)
			VALUES (?, CAST(? AS inet), 'manual', 'removed', now())
			""".trimIndent(),
			orgId,
			"198.51.100.25",
		)

		val result = mockMvc.perform(
			get("/api/v1/admin/orgs/$orgId")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(orgId.toString()))
			.andExpect(jsonPath("$.name").value("Acme"))
			.andExpect(jsonPath("$.state").value("ONLINE"))
			.andExpect(jsonPath("$.owner.user_id").value(ownerId.toString()))
			.andExpect(jsonPath("$.members.length()").value(2))
			.andExpect(jsonPath("$.members[0].role").value("owner"))
			.andExpect(jsonPath("$.proxy.running_config_version").value(2))
			.andExpect(jsonPath("$.proxy.latest_config_version").value(2))
			.andExpect(jsonPath("$.proxy.config_status").value("applied"))
			.andExpect(jsonPath("$.proxy.active_ban_count").value(1))
			.andExpect(jsonPath("$.proxy.last_heartbeat_at").isNotEmpty)
			.andReturn()

		val body = result.response.contentAsString
		assertFalse(body.contains("198.51.100.23"), "ban IPs must not leak")
		assertFalse(body.contains("\"document\""), "config contents must not leak")

		assertEquals(
			1L,
			jdbc.queryForObject(
				"SELECT count(*) FROM platform_audit WHERE action = 'org.view' AND target_id = ?",
				Long::class.java,
				orgId.toString(),
			),
		)
	}

	@Test
	fun `org detail reports pending and rejected config states`() {
		val (_, token) = adminSession()
		val pendingOrg = createOrg("Pending", createUser(uniqueEmail("pending-owner")))
		val rejectedOrg = createOrg("Rejected", createUser(uniqueEmail("rejected-owner")))
		createConfig(rejectedOrg, version = 1, appliedVersion = null, rejected = true)

		mockMvc.perform(
			get("/api/v1/admin/orgs/$pendingOrg")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.proxy.latest_config_version").isEmpty)
			.andExpect(jsonPath("$.proxy.config_status").value("pending"))

		mockMvc.perform(
			get("/api/v1/admin/orgs/$rejectedOrg")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.proxy.latest_config_version").value(1))
			.andExpect(jsonPath("$.proxy.config_status").value("rejected"))
	}

	@Test
	fun `unknown org returns not found`() {
		val (_, token) = adminSession()

		mockMvc.perform(
			get("/api/v1/admin/orgs/${UUID.randomUUID()}")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
	}

	private fun search(token: String, q: String) =
		mockMvc.perform(
			get("/api/v1/admin/orgs")
				.param("q", q)
				.header(HttpHeaders.AUTHORIZATION, "Bearer $token"),
		)

	private fun adminSession(): Pair<UUID, String> {
		val email = uniqueEmail("admin")
		val userId = createUser(email, "Platform Admin", platformAdmin = true)
		return userId to accessToken(login(email))
	}

	private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"
}
