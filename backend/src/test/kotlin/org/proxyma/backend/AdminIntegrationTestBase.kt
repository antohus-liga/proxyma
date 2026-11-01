package org.proxyma.backend

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.BeforeEach
import org.proxyma.backend.auth.RefreshTokenService
import org.proxyma.backend.user.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration::class)
@TestPropertySource(
	properties = [
		"proxyma.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"proxyma.auth.argon2.memory-kibibytes=1024",
		"proxyma.auth.argon2.iterations=1",
		"proxyma.auth.cookie.secure=false",
		"proxyma.auth.rate-limit.register-per-minute=10000",
		"proxyma.auth.rate-limit.login-per-minute=10000",
		"proxyma.auth.rate-limit.account-login-per-minute=10000",
		"proxyma.auth.rate-limit.refresh-per-minute=10000",
		"proxyma.admin.enabled=true",
		"proxyma.admin.rate-limit.requests-per-minute=10000",
		"proxyma.admin.rate-limit.risky-per-minute=10000",
	],
)
abstract class AdminIntegrationTestBase {

	@Autowired
	protected lateinit var mockMvc: MockMvc

	@Autowired
	protected lateinit var jdbc: JdbcTemplate

	@Autowired
	protected lateinit var users: UserRepository

	@Autowired
	protected lateinit var refreshTokens: RefreshTokenService

	@BeforeEach
	fun cleanDatabase() {
		jdbc.update("DELETE FROM platform_audit")
		jdbc.update("DELETE FROM org_audit_entries")
		jdbc.update("DELETE FROM org_config_state")
		jdbc.update("DELETE FROM org_configs")
		jdbc.update("DELETE FROM bans")
		jdbc.update("DELETE FROM org_memberships")
		jdbc.update("DELETE FROM organisations")
		jdbc.update("DELETE FROM refresh_tokens")
		jdbc.update("DELETE FROM users")
	}

	protected fun register(
		email: String,
		displayName: String = "Test User",
		password: String = VALID_PASSWORD,
	) {
		mockMvc.perform(
			post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"email":"$email","display_name":"$displayName","password":"$password"}""",
				),
		).andExpect(status().isCreated)
	}

	protected fun login(
		email: String,
		password: String = VALID_PASSWORD,
	): MvcResult =
		mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"$email","password":"$password"}"""),
		).andExpect(status().isOk).andReturn()

	protected fun accessToken(result: MvcResult): String =
		JsonPath.read(result.response.contentAsString, "$.access_token")

	protected fun createUser(
		email: String,
		displayName: String = "Test User",
		platformAdmin: Boolean = false,
		password: String = VALID_PASSWORD,
	): UUID {
		register(email, displayName, password)
		val user = users.findByEmail(email)!!
		if (platformAdmin) {
			user.platformAdmin = true
			users.save(user)
		}
		return user.id
	}

	protected fun adminToken(
		email: String = "admin-${UUID.randomUUID()}@example.com",
	): String {
		createUser(email, "Platform Admin", platformAdmin = true)
		return accessToken(login(email))
	}

	protected fun createOrg(
		name: String = "Acme",
		ownerId: UUID,
		state: String = "ONLINE",
	): UUID {
		val orgId = UUID.randomUUID()
		jdbc.update(
			"""
			INSERT INTO organisations (id, name, state)
			VALUES (?, ?, ?)
			""".trimIndent(),
			orgId,
			name,
			state,
		)
		addMembership(orgId, ownerId, "owner")
		return orgId
	}

	protected fun addMembership(orgId: UUID, userId: UUID, role: String) {
		jdbc.update(
			"""
			INSERT INTO org_memberships (org_id, user_id, role)
			VALUES (?, ?, ?)
			""".trimIndent(),
			orgId,
			userId,
			role,
		)
	}

	protected fun createConfig(
		orgId: UUID,
		version: Int = 1,
		appliedVersion: Int? = version,
		rejected: Boolean = false,
	) {
		jdbc.update(
			"""
			INSERT INTO org_configs (
			    org_id, version, schema_version, document, content_hash, source, rejected_at
			) VALUES (
			    ?, ?, 1, '{}'::jsonb, ?, 'user',
			    CASE WHEN CAST(? AS boolean) THEN now() ELSE NULL END
			)
			""".trimIndent(),
			orgId,
			version,
			"hash-$version",
			rejected,
		)
		jdbc.update(
			"""
			INSERT INTO org_config_state (
			    org_id, latest_version, published_version, applied_version
			) VALUES (?, ?, ?, ?)
			""".trimIndent(),
			orgId,
			version,
			version,
			appliedVersion,
		)
	}

	protected fun createBan(orgId: UUID, ip: String = "198.51.100.23") {
		jdbc.update(
			"""
			INSERT INTO bans (org_id, ip, source, reason, expires_at)
			VALUES (?, CAST(? AS inet), 'manual', 'test ban', now() + interval '1 hour')
			""".trimIndent(),
			orgId,
			ip,
		)
	}

	protected fun issueRefreshToken(userId: UUID): String =
		refreshTokens.issue(userId).rawToken

	@Suppress("UNCHECKED_CAST")
	protected fun <T : MockHttpServletRequestBuilder> T.from(ip: String): T =
		with { it.apply { remoteAddr = ip } } as T

	companion object {
		protected const val VALID_PASSWORD = "correct horse battery staple"
	}
}
