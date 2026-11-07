package org.proxyma.backend.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.proxyma.backend.config.HttpProperties
import org.proxyma.backend.config.ProxymaProperties
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClientIpResolverTest {

	private val resolver = ClientIpResolver(
		ProxymaProperties(
			http = HttpProperties(
				trustedProxies = listOf("127.0.0.1/32", "::1/128", "10.0.0.1"),
			),
		),
	)

	@Test
	fun `direct clients keep their connection address even with a forwarded header`() {
		assertEquals("203.0.113.5", resolver.resolve("203.0.113.5", "10.1.2.3"))
	}

	@Test
	fun `trusted proxy uses the single forwarded client`() {
		assertEquals("203.0.113.5", resolver.resolve("127.0.0.1", "203.0.113.5"))
	}

	@Test
	fun `trusted proxy skips trusted hops from the right`() {
		assertEquals(
			"203.0.113.5",
			resolver.resolve("127.0.0.1", "192.0.2.9, 203.0.113.5, 10.0.0.1"),
		)
	}

	@Test
	fun `a client-supplied prefix in the forwarded chain is ignored`() {
		assertEquals("203.0.113.5", resolver.resolve("127.0.0.1", "10.9.9.9,203.0.113.5"))
	}

	@Test
	fun `ipv4 ports are stripped`() {
		assertEquals("203.0.113.5", resolver.resolve("127.0.0.1", "203.0.113.5:1234"))
	}

	@Test
	fun `bracketed ipv6 with a port is accepted`() {
		assertEquals("2001:db8::1", resolver.resolve("::1", "[2001:db8::1]:443"))
	}

	@Test
	fun `bare ipv6 is accepted`() {
		assertEquals("2001:db8::1", resolver.resolve("127.0.0.1", "2001:db8::1"))
	}

	@Test
	fun `unknown forwarded values fall back to the connection address`() {
		assertEquals("127.0.0.1", resolver.resolve("127.0.0.1", "unknown"))
		assertEquals("127.0.0.1", resolver.resolve("127.0.0.1", "example.com"))
		assertEquals("127.0.0.1", resolver.resolve("127.0.0.1", ""))
	}

	@Test
	fun `missing values are preserved`() {
		assertNull(resolver.resolve(null, "203.0.113.5"))
		assertEquals("", resolver.resolve("", "203.0.113.5"))
	}

	@Test
	fun `an invalid trusted proxy entry fails fast`() {
		assertThrows<IllegalArgumentException> {
			ClientIpResolver(
				ProxymaProperties(
					http = HttpProperties(trustedProxies = listOf("not-a-network")),
				),
			)
		}
	}
}
