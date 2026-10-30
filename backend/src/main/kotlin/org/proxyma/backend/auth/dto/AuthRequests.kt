package org.proxyma.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
	@field:NotBlank(message = "must not be blank")
	@field:Email(message = "must be a valid email address")
	@field:Size(max = 254, message = "must be at most 254 characters")
	val email: String,

	@field:NotBlank(message = "must not be blank")
	@field:Size(min = 1, max = 80, message = "must be between 1 and 80 characters")
	val displayName: String,

	@field:NotBlank(message = "must not be blank")
	val password: String,
)

data class LoginRequest(
	@field:NotBlank(message = "must not be blank")
	@field:Size(max = 254, message = "must be at most 254 characters")
	val email: String,

	@field:NotBlank(message = "must not be blank")
	@field:Size(max = 128, message = "must be at most 128 characters")
	val password: String,
)
