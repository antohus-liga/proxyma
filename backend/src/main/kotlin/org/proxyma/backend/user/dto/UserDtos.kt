package org.proxyma.backend.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.proxyma.backend.user.User
import java.util.UUID

data class UserResponse(
	val id: UUID,
	val email: String,
	val displayName: String,
	@get:JsonProperty("is_platform_admin")
	@param:JsonProperty("is_platform_admin")
	val platformAdmin: Boolean,
) {
	companion object {
		fun from(user: User): UserResponse = UserResponse(
			id = user.id,
			email = user.email,
			displayName = user.displayName,
			platformAdmin = user.platformAdmin,
		)
	}
}

data class UpdateProfileRequest(
	@field:NotBlank(message = "must not be blank")
	@field:Size(min = 1, max = 80, message = "must be between 1 and 80 characters")
	val displayName: String,
)

data class ChangePasswordRequest(
	@field:NotBlank(message = "must not be blank")
	val currentPassword: String,

	@field:NotBlank(message = "must not be blank")
	val newPassword: String,
)
