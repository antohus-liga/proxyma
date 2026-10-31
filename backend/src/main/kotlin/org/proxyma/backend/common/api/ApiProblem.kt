package org.proxyma.backend.common.api

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

/**
 * The single source of truth for `application/problem+json` bodies.
 *
 * Errors used to be rendered by three independent paths: MVC exceptions were
 * serialised from `ProblemDetail`, security-filter exceptions were assembled by
 * hand, and CORS rejections were plain text. The documents disagreed — one
 * dropped `type`, another dropped `instance` — so clients could not rely on a
 * stable shape. Every error path now renders through [body].
 */
object ApiProblem {

	/**
	 * Builds the body in a fixed key order: `type`, `title`, `status`,
	 * `detail`, `instance`, `code`, then `errors` when present.
	 *
	 * `type` is always present, even though RFC 9457 allows omitting it and
	 * defaults to `about:blank`; clients should not have to guess.
	 */
	fun body(
		status: HttpStatusCode,
		code: String,
		detail: String,
		instance: String,
		errors: List<ApiError> = emptyList(),
	): Map<String, Any?> = buildMap {
		put("type", "about:blank")
		put("title", titleFor(status))
		put("status", status.value())
		put("detail", detail)
		put("instance", instance)
		put("code", code)
		if (errors.isNotEmpty()) {
			put("errors", errors)
		}
	}

	fun titleFor(status: HttpStatusCode): String =
		(status as? HttpStatus)?.reasonPhrase ?: "Error"
}
