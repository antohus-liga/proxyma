package org.proxyma.backend.common.api

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(ApiException::class)
	fun handleApiException(ex: ApiException): ResponseEntity<ProblemDetail> =
		problem(ex.status, ex.code, ex.message, ex.errors, ex.headers)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
		val errors = ex.bindingResult.fieldErrors.map { ApiError(pointer(it.field), it.defaultMessage ?: "is invalid") }
		return problem(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"VALIDATION_FAILED",
			"The request has ${errors.size} problem${if (errors.size == 1) "" else "s"}.",
			errors,
		)
	}

	@ExceptionHandler(BindException::class)
	fun handleBind(ex: BindException): ResponseEntity<ProblemDetail> {
		val errors = ex.bindingResult.fieldErrors.map { ApiError(pointer(it.field), it.defaultMessage ?: "is invalid") }
		return problem(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"VALIDATION_FAILED",
			"The request has ${errors.size} problem${if (errors.size == 1) "" else "s"}.",
			errors,
		)
	}

	@ExceptionHandler(ConstraintViolationException::class)
	fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ProblemDetail> {
		val errors = ex.constraintViolations.map { ApiError("/${it.propertyPath}", it.message) }
		return problem(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"VALIDATION_FAILED",
			"The request has ${errors.size} problem${if (errors.size == 1) "" else "s"}.",
			errors,
		)
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> =
		problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body could not be read.")

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> =
		problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "The parameter '${ex.name}' is invalid.")

	@ExceptionHandler(NoResourceFoundException::class)
	fun handleNotFound(ex: NoResourceFoundException): ResponseEntity<ProblemDetail> =
		problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource does not exist.")

	@ExceptionHandler(DataIntegrityViolationException::class)
	fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ProblemDetail> {
		log.debug("Database constraint violated", ex)
		return problem(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with the current state.")
	}

	@ExceptionHandler(AccessDeniedException::class)
	fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ProblemDetail> =
		problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to do that.")

	@ExceptionHandler(Exception::class)
	fun handleUnexpected(ex: Exception): ResponseEntity<ProblemDetail> {
		log.error("Unhandled exception", ex)
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong.")
	}

	private fun pointer(field: String): String =
		"/" + field.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

	private fun problem(
		status: HttpStatusCode,
		code: String,
		detail: String,
		errors: List<ApiError> = emptyList(),
		headers: HttpHeaders = HttpHeaders(),
	): ResponseEntity<ProblemDetail> {
		val problem = ProblemDetail.forStatusAndDetail(status, detail)
		problem.title = (status as? HttpStatus)?.reasonPhrase ?: "Error"
		problem.setProperty("code", code)
		if (errors.isNotEmpty()) {
			problem.setProperty("errors", errors)
		}
		return ResponseEntity.status(status)
			.headers(headers)
			.contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.body(problem)
	}
}
