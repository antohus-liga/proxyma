package org.proxyma.backend.admin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.proxyma.backend.config.AdminProperties
import org.proxyma.backend.config.ProxymaProperties
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminNetworkMatcherTest {

	@Test
	fun `an empty allowlist allows every source`() {
		val matcher = AdminNetworkMatcher(ProxymaProperties())

		assertTrue(matcher.allows("127.0.0.1"))
		assertTrue(matcher.allows("203.0.113.5"))
		assertTrue(matcher.allows("2001:db8::1"))
		assertTrue(matcher.allows(null))
	}

	@Test
	fun `only addresses inside the configured networks are allowed`() {
		val matcher = AdminNetworkMatcher(
			ProxymaProperties(
				admin = AdminProperties(
					allowedCidrs = listOf("10.0.0.0/8", "192.168.1.0/24", "2001:db8::/32"),
				),
			),
		)

		assertTrue(matcher.allows("10.1.2.3"))
		assertTrue(matcher.allows("10.255.255.255"))
		assertTrue(matcher.allows("192.168.1.42"))
		assertTrue(matcher.allows("2001:db8::1"))

		assertFalse(matcher.allows("127.0.0.1"))
		assertFalse(matcher.allows("11.0.0.1"))
		assertFalse(matcher.allows("192.168.2.1"))
		assertFalse(matcher.allows("2001:db9::1"))
		assertFalse(matcher.allows(null))
		assertFalse(matcher.allows(""))
	}

	@Test
	fun `a single address entry behaves like a host route`() {
		val matcher = AdminNetworkMatcher(
			ProxymaProperties(
				admin = AdminProperties(allowedCidrs = listOf("203.0.113.10")),
			),
		)

		assertTrue(matcher.allows("203.0.113.10"))
		assertFalse(matcher.allows("203.0.113.11"))
	}

	@Test
	fun `an invalid CIDR fails at startup instead of silently allowing everything`() {
		assertThrows<IllegalArgumentException> {
			AdminNetworkMatcher(
				ProxymaProperties(
					admin = AdminProperties(allowedCidrs = listOf("not-a-network")),
				),
			)
		}
	}
}
