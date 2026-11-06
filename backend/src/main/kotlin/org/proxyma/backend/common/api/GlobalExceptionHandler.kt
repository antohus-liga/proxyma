package org.proxyma.backend.common.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Renders MVC-layer failures. Security-filter failures go through
 * [ProblemResponseWriter] instead, but both use [ApiProblem] so the document
 * shape is identical.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(ApiException::class)
	fun handleApiException(ex: ApiException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> =
		problem(ex.status, ex.code, ex.message, request, ex.errors, ex.headers)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(
		ex: MethodArgumentNotValidException,
		request: HttpServletRequest,
	): ResponseEntity<Map<String, Any?>> {
		val errors = ex.bindingResult.fieldErrors.map { ApiError(pointer(it.field), it.defaultMessage ?: "is invalid") }
		return problem(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"VALIDATION_FAILED",
			"The request has ${errors.size} problem${if (errors.size == 1) "" else "s"}.",
			request,
			errors,
		)
	}

	@ExceptionHandler(BindException::class)
	fun handleBind(ex: BindException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
		val errors = ex.bindingResult.fieldErrors.map { ApiError(pointer(it.field), it.defaultMessage ?: "is invalid") }
		return problem(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"VALIDATION_FAILED",
			"The request has ${errors.size} problem${if (errors.size == 1) "" else "s"}.",
			request,
			errors,
		)
	}

	@ExceptionHandler(ConstraintViolationException::class)
	fun handleConstraintViolation(
		ex: ConstraintViolationException,
		request: HttpServletRequest,
	): ResponseEntity<Map<String, Any?>> {
		val errors = ex.constraintViolations.map { ApiError("/${it.propertyPath}", it.message) }
		return problem(
			HttpStatus.UNPROCESSABLE_CONTENT,
			"VALIDATION_FAILED",
			"The request has ${errors.size} problem${if (errors.size == 1) "" else "s"}.",
			request,
			errors,
		)
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleUnreadable(
		ex: HttpMessageNotReadableException,
		request: HttpServletRequest,
	): ResponseEntity<Map<String, Any?>> =
		problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body could not be read.", request)

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleTypeMismatch(
		ex: MethodArgumentTypeMismatchException,
		request: HttpServletRequest,
	): ResponseEntity<Map<String, Any?>> =
		problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "The parameter '${ex.name}' is invalid.", request)

	@ExceptionHandler(NoResourceFoundException::class)
	fun handleNotFound(
		ex: NoResourceFoundException,
		request: HttpServletRequest,
	): ResponseEntity<Map<String, Any?>> =
		problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource does not exist.", request)

	@ExceptionHandler(DataIntegrityViolationException::class)
	fun handleDataIntegrity(
		ex: DataIntegrityViolationException,
		request: HttpServletRequest,
	): ResponseEntity<Map<String, Any?>> {
		log.debug("Database constraint violated", ex)
		return problem(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with the current state.", request)
	}

	@ExceptionHandler(AccessDeniedException::class)
	fun handleAccessDenied(
		ex: AccessDeniedException,
		request: HttpServletRequest,
	): ResponseEntity<Map<String, Any?>> =
		problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to do that.", request)

	@ExceptionHandler(Exception::class)
	fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
		log.error("Unhandled exception", ex)
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong.", request)
	}

	private fun pointer(field: String): String =
		"/" + field.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

	private fun problem(
		status: HttpStatusCode,
		code: String,
		detail: String,
		request: HttpServletRequest,
		errors: List<ApiError> = emptyList(),
		headers: HttpHeaders = HttpHeaders(),
	): ResponseEntity<Map<String, Any?>> =
		ResponseEntity.status(status)
			.headers(headers)
			.contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.body(ApiProblem.body(status, code, detail, request.requestURI, errors))
}
