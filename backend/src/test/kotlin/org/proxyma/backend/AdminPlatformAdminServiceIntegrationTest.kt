package org.proxyma.backend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.proxyma.backend.admin.AdminCommandRunner
import org.proxyma.backend.admin.PlatformAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * The operator commands are the only way to create a platform admin. They are
 * deliberately not reachable over HTTP, so they are exercised through the
 * service the command runner calls.
 */
class AdminPlatformAdminServiceIntegrationTest : AdminIntegrationTestBase() {

	@Autowired
	private lateinit var platformAdmins: PlatformAdminService

	@Test
	fun `grant sets the flag, bumps the token version and writes an operator audit entry`() {
		val email = uniqueEmail("operator-grant")
		val userId = createUser(email, "Future Admin")
		val before = users.findById(userId).orElseThrow().tokenVersion

		val result = platformAdmins.grant(email)

		assertTrue(result.changed)
		val user = users.findById(userId).orElseThrow()
		assertTrue(user.platformAdmin)
		assertEquals(before + 1, user.tokenVersion)

		val entry = jdbc.queryForMap(
			"""
			SELECT admin_user_id, action, target_type, target_id, source_ip::text AS source_ip
			FROM platform_audit
			WHERE action = 'platform_admin.grant'
			""".trimIndent(),
		)
		assertNull(entry["admin_user_id"])
		assertEquals("user", entry["target_type"])
		assertEquals(userId.toString(), entry["target_id"])
		assertNull(entry["source_ip"])
	}

	@Test
	fun `grant is idempotent and does not bump the token version again`() {
		val email = uniqueEmail("operator-idempotent")
		val userId = createUser(email, "Future Admin")
		platformAdmins.grant(email)
		val afterFirst = users.findById(userId).orElseThrow().tokenVersion

		val second = platformAdmins.grant(email)

		assertFalse(second.changed)
		assertEquals(afterFirst, users.findById(userId).orElseThrow().tokenVersion)
		assertEquals(
			1L,
			jdbc.queryForObject(
				"SELECT count(*) FROM platform_audit WHERE action = 'platform_admin.grant' AND target_id = ?",
				Long::class.java,
				userId.toString(),
			),
		)
	}

	@Test
	fun `revoke clears the flag, bumps the token version and writes an operator audit entry`() {
		val email = uniqueEmail("operator-revoke")
		val userId = createUser(email, "Former Admin", platformAdmin = true)
		val before = users.findById(userId).orElseThrow().tokenVersion

		val result = platformAdmins.revoke(email)

		assertTrue(result.changed)
		val user = users.findById(userId).orElseThrow()
		assertFalse(user.platformAdmin)
		assertEquals(before + 1, user.tokenVersion)
		assertEquals(
			1L,
			jdbc.queryForObject(
				"SELECT count(*) FROM platform_audit WHERE action = 'platform_admin.revoke' AND target_id = ?",
				Long::class.java,
				userId.toString(),
			),
		)
	}

	@Test
	fun `revoke is idempotent for a normal user`() {
		val email = uniqueEmail("operator-normal")
		val userId = createUser(email)
		val before = users.findById(userId).orElseThrow().tokenVersion

		val result = platformAdmins.revoke(email)

		assertFalse(result.changed)
		assertEquals(before, users.findById(userId).orElseThrow().tokenVersion)
		assertEquals(
			0L,
			jdbc.queryForObject(
				"SELECT count(*) FROM platform_audit WHERE action = 'platform_admin.revoke'",
				Long::class.java,
			),
		)
	}

	@Test
	fun `an unknown email is rejected`() {
		assertThrows<IllegalArgumentException> {
			platformAdmins.grant(uniqueEmail("missing"))
		}
	}

	@Test
	fun `a token issued before a grant does not silently become an admin token`() {
		val email = uniqueEmail("token-grant")
		createUser(email)
		val oldToken = accessToken(login(email))

		platformAdmins.grant(email)

		// The token version changed, so the old token is rejected even though
		// the account is now an admin.
		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $oldToken"),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))

		val freshToken = accessToken(login(email))
		mockMvc.perform(
			get("/api/v1/admin/stats")
				.header(HttpHeaders.AUTHORIZATION, "Bearer $freshToken"),
		).andExpect(status().isOk)
	}

	@Test
	fun `the command runner rejects a missing email with exit code 2`() {
		val runner = AdminCommandRunner(platformAdmins)

		runner.run(arguments("grant-admin"))

		assertEquals(2, runner.exitCode)
	}

	@Test
	fun `the command runner grants an admin and reports success`() {
		val email = uniqueEmail("runner-grant")
		val userId = createUser(email)
		val runner = AdminCommandRunner(platformAdmins)

		runner.run(arguments("grant-admin", email))

		assertEquals(0, runner.exitCode)
		assertTrue(users.findById(userId).orElseThrow().platformAdmin)
	}

	@Test
	fun `the command runner reports a failed command with exit code 1`() {
		val runner = AdminCommandRunner(platformAdmins)

		runner.run(arguments("revoke-admin", uniqueEmail("missing")))

		assertEquals(1, runner.exitCode)
	}

	private fun arguments(vararg values: String): ApplicationArguments =
		object : ApplicationArguments {
			override fun getSourceArgs(): Array<String> = arrayOf(*values)
			override fun getOptionNames(): Set<String> = emptySet()
			override fun containsOption(name: String): Boolean = false
			override fun getOptionValues(name: String): List<String>? = null
			override fun getNonOptionArgs(): List<String> = values.toList()
		}

	private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID()}@example.com"
}
