package org.proxyma.backend.auth.dto

import org.proxyma.backend.user.dto.UserResponse

data class LoginResponse(
	val accessToken: String,
	val tokenType: String = "Bearer",
	val expiresIn: Long,
	val user: UserResponse,
)
