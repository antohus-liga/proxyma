package org.proxyma.backend.admin

import org.proxyma.backend.common.api.ApiException
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Keyset position for admin lists. The cursor is opaque to clients and is
 * encoded as base64url("<created_at>|<id>"). `created_at` is an ISO-8601
 * instant with nanosecond precision, so rows created in the same millisecond
 * are still paged correctly.
 */
data class AdminCursor(
	val createdAt: Instant,
	val id: UUID,
)

object AdminCursorCodec {

	private val encoder = Base64.getUrlEncoder().withoutPadding()
	private val decoder = Base64.getUrlDecoder()

	fun encode(createdAt: Instant, id: UUID): String =
		encoder.encodeToString("$createdAt|$id".toByteArray(Charsets.UTF_8))

	fun decode(cursor: String): AdminCursor {
		val decoded = runCatching {
			String(decoder.decode(cursor), Charsets.UTF_8)
		}.getOrElse { throw invalidCursor() }

		val separator = decoded.lastIndexOf('|')
		if (separator <= 0 || separator == decoded.lastIndex) {
			throw invalidCursor()
		}

		val createdAt = runCatching { Instant.parse(decoded.substring(0, separator)) }
			.getOrElse { throw invalidCursor() }
		val id = runCatching { UUID.fromString(decoded.substring(separator + 1)) }
			.getOrElse { throw invalidCursor() }

		return AdminCursor(createdAt, id)
	}

	private fun invalidCursor() = ApiException(
		status = HttpStatus.BAD_REQUEST,
		code = "INVALID_CURSOR",
		message = "The pagination cursor is invalid.",
	)
}
