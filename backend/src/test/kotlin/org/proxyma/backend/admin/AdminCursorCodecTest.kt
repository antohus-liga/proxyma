package org.proxyma.backend.admin

import org.junit.jupiter.api.Test
import org.proxyma.backend.common.api.ApiException
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminCursorCodecTest {

	private val createdAt = Instant.parse("2026-10-30T12:34:56.123456Z")
	private val id = UUID.fromString("5f0c9d8e-1a2b-4c3d-8e9f-0123456789ab")

	@Test
	fun `round trips the timestamp and id`() {
		val cursor = AdminCursorCodec.encode(createdAt, id)
		val decoded = AdminCursorCodec.decode(cursor)

		assertEquals(createdAt, decoded.createdAt)
		assertEquals(id, decoded.id)
	}

	@Test
	fun `the cursor is opaque and URL safe`() {
		val cursor = AdminCursorCodec.encode(createdAt, id)

		assertEquals(false, cursor.contains('|'))
		assertEquals(false, cursor.contains('+'))
		assertEquals(false, cursor.contains('/'))
		assertEquals(false, cursor.contains('='))
	}

	@Test
	fun `malformed cursors are rejected with INVALID_CURSOR`() {
		listOf(
			"not-base64!",
			"",
			"bm90LWEtY3Vyc29y", // "not-a-cursor"
			"bm90LWEtY3Vyc29yfG5vdC1hLXV1aWQ", // "not-a-cursor|not-a-uuid"
		).forEach { value ->
			val ex = assertFailsWith<ApiException> { AdminCursorCodec.decode(value) }
			assertEquals("INVALID_CURSOR", ex.code)
		}
	}
}
