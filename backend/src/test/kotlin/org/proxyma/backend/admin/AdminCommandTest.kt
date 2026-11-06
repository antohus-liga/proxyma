package org.proxyma.backend.admin

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminCommandTest {

	@Test
	fun `recognises the two operator commands`() {
		assertTrue(AdminCommand.isCommand(arrayOf("grant-admin", "ana@example.com")))
		assertTrue(AdminCommand.isCommand(arrayOf("revoke-admin", "ana@example.com")))
	}

	@Test
	fun `ignores ordinary startup arguments`() {
		assertFalse(AdminCommand.isCommand(emptyArray()))
		assertFalse(AdminCommand.isCommand(arrayOf("--server.port=8080")))
		assertFalse(AdminCommand.isCommand(arrayOf("ana@example.com")))
	}

	@Test
	fun `only looks at the first non-option argument`() {
		assertTrue(AdminCommand.isCommand(arrayOf("--spring.profiles.active=prod", "grant-admin", "ana@example.com")))
		assertFalse(AdminCommand.isCommand(arrayOf("--spring.profiles.active=prod", "not-a-command")))
	}
}
