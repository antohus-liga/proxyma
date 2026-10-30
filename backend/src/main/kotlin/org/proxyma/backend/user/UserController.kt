package org.proxyma.backend.user

import org.proxyma.backend.security.ProxymaPrincipal
import org.proxyma.backend.user.dto.ChangePasswordRequest
import org.proxyma.backend.user.dto.UpdateProfileRequest
import org.proxyma.backend.user.dto.UserResponse
import org.proxyma.backend.auth.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
	private val authService: AuthService,
) {

	@GetMapping("/me")
	fun me(@AuthenticationPrincipal principal: ProxymaPrincipal): UserResponse =
		UserResponse(
			id = principal.userId,
			email = principal.email,
			displayName = principal.displayName,
			platformAdmin = principal.platformAdmin,
		)

	@PatchMapping("/me")
	fun updateMe(
		@AuthenticationPrincipal principal: ProxymaPrincipal,
		@Valid @RequestBody request: UpdateProfileRequest,
	): UserResponse = authService.updateDisplayName(principal.userId, request.displayName)

	@PostMapping("/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun changePassword(
		@AuthenticationPrincipal principal: ProxymaPrincipal,
		@Valid @RequestBody request: ChangePasswordRequest,
	) {
		authService.changePassword(principal.userId, request)
	}
}
