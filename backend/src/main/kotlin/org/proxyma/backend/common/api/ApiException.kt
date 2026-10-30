package org.proxyma.backend.common.api

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

data class ApiError(
	val path: String,
	val message: String,
)

class ApiException(
	val status: HttpStatus,
	val code: String,
	override val message: String,
	val errors: List<ApiError> = emptyList(),
	val headers: HttpHeaders = HttpHeaders(),
) : RuntimeException(message)
