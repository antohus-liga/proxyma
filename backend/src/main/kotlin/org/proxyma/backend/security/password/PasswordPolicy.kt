package org.proxyma.backend.security.password

import org.proxyma.backend.common.api.ApiError
import org.proxyma.backend.common.api.ApiException
import org.proxyma.backend.config.ProxymaProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class PasswordPolicy(
	private val properties: ProxymaProperties,
	private val commonPasswords: CommonPasswords,
) {

	fun validate(password: String, field: String = "password"): List<ApiError> {
		val errors = mutableListOf<ApiError>()
		val length = password.codePointCount(0, password.length)
		val passwordProperties = properties.auth.password

		if (length < passwordProperties.minLength) {
			errors += ApiError("/$field", "must be at least ${passwordProperties.minLength} characters")
		}
		if (length > passwordProperties.maxLength) {
			errors += ApiError("/$field", "must be at most ${passwordProperties.maxLength} characters")
		}
		if (commonPasswords.contains(password)) {
			errors += ApiError("/$field", "is too common")
		}

		return errors
	}

	fun validateOrThrow(password: String, field: String = "password") {
		val errors = validate(password, field)
		if (errors.isNotEmpty()) {
			throw ApiException(
				status = HttpStatus.UNPROCESSABLE_CONTENT,
				code = "WEAK_PASSWORD",
				message = "The password does not meet the requirements.",
				errors = errors,
			)
		}
	}
}
