package org.proxyma.backend.admin

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.proxyma.backend.admin.dto.AdminAuditListResponse
import org.proxyma.backend.admin.dto.AdminForceLogoutRequest
import org.proxyma.backend.admin.dto.AdminOrgDetail
import org.proxyma.backend.admin.dto.AdminOrgListResponse
import org.proxyma.backend.admin.dto.AdminStatsResponse
import org.proxyma.backend.admin.dto.AdminSuspendRequest
import org.proxyma.backend.admin.dto.AdminUnsuspendRequest
import org.proxyma.backend.admin.dto.AdminUserDetail
import org.proxyma.backend.admin.dto.AdminUserListResponse
import org.proxyma.backend.config.ProxymaProperties
import org.proxyma.backend.security.ProxymaPrincipal
import org.proxyma.backend.security.ratelimit.RateLimitService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping(AdminAccessFilter.ADMIN_PATH)
@Validated
class AdminController(
	private val adminService: AdminService,
	private val rateLimit: RateLimitService,
	private val properties: ProxymaProperties,
) {

	@GetMapping("/stats")
	fun stats(): AdminStatsResponse = adminService.stats()

	@GetMapping("/users")
	fun listUsers(
		@RequestParam(name = "q", required = false) q: String?,
		@RequestParam(name = "status", required = false) status: String?,
		@RequestParam(name = "limit", defaultValue = "50")
		@Min(1, message = "must be at least 1")
		@Max(200, message = "must be at most 200")
		limit: Int,
		@RequestParam(name = "cursor", required = false) cursor: String?,
	): AdminUserListResponse = adminService.listUsers(
		q = q,
		status = status,
		cursor = cursor?.let(AdminCursorCodec::decode),
		limit = limit,
	)

	@GetMapping("/users/{user_id}")
	fun getUser(
		@AuthenticationPrincipal principal: ProxymaPrincipal,
		@PathVariable(name = "user_id") userId: UUID,
		request: HttpServletRequest,
	): AdminUserDetail = adminService.getUser(
		adminUserId = principal.userId,
		userId = userId,
		sourceIp = request.remoteAddr,
	)

	@PostMapping("/users/{user_id}/suspend")
	fun suspend(
		@AuthenticationPrincipal principal: ProxymaPrincipal,
		@PathVariable(name = "user_id") userId: UUID,
		@Valid @RequestBody request: AdminSuspendRequest,
		httpRequest: HttpServletRequest,
		httpResponse: HttpServletResponse,
	): AdminUserDetail {
		enforceRiskyLimit(principal.userId, httpResponse)
		return adminService.suspend(
			adminUserId = principal.userId,
			userId = userId,
			request = request,
			sourceIp = httpRequest.remoteAddr,
		)
	}

	@PostMapping("/users/{user_id}/unsuspend")
	fun unsuspend(
		@AuthenticationPrincipal principal: ProxymaPrincipal,
		@PathVariable(name = "user_id") userId: UUID,
		@Valid @RequestBody request: AdminUnsuspendRequest,
		httpRequest: HttpServletRequest,
	): AdminUserDetail = adminService.unsuspend(
		adminUserId = principal.userId,
		userId = userId,
		request = request,
		sourceIp = httpRequest.remoteAddr,
	)

	@PostMapping("/users/{user_id}/force-logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun forceLogout(
		@AuthenticationPrincipal principal: ProxymaPrincipal,
		@PathVariable(name = "user_id") userId: UUID,
		@Valid @RequestBody request: AdminForceLogoutRequest,
		httpRequest: HttpServletRequest,
		httpResponse: HttpServletResponse,
	) {
		enforceRiskyLimit(principal.userId, httpResponse)
		adminService.forceLogout(
			adminUserId = principal.userId,
			userId = userId,
			request = request,
			sourceIp = httpRequest.remoteAddr,
		)
	}

	@GetMapping("/orgs")
	fun listOrgs(
		@RequestParam(name = "q", required = false) q: String?,
		@RequestParam(name = "state", required = false) state: String?,
		@RequestParam(name = "limit", defaultValue = "50")
		@Min(1, message = "must be at least 1")
		@Max(200, message = "must be at most 200")
		limit: Int,
		@RequestParam(name = "cursor", required = false) cursor: String?,
	): AdminOrgListResponse = adminService.listOrgs(
		q = q,
		state = state,
		cursor = cursor?.let(AdminCursorCodec::decode),
		limit = limit,
	)

	@GetMapping("/orgs/{org_id}")
	fun getOrg(
		@AuthenticationPrincipal principal: ProxymaPrincipal,
		@PathVariable(name = "org_id") orgId: UUID,
		request: HttpServletRequest,
	): AdminOrgDetail = adminService.getOrg(
		adminUserId = principal.userId,
		orgId = orgId,
		sourceIp = request.remoteAddr,
	)

	@GetMapping("/audit")
	fun listAudit(
		@RequestParam(name = "from", required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		from: Instant?,
		@RequestParam(name = "to", required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		to: Instant?,
		@RequestParam(name = "admin_user_id", required = false) adminUserId: UUID?,
		@RequestParam(name = "action", required = false) action: String?,
		@RequestParam(name = "limit", defaultValue = "50")
		@Min(1, message = "must be at least 1")
		@Max(200, message = "must be at most 200")
		limit: Int,
		@RequestParam(name = "cursor", required = false) cursor: String?,
	): AdminAuditListResponse = adminService.listAudit(
		from = from,
		to = to,
		adminUserId = adminUserId,
		action = action,
		cursor = cursor?.let(AdminCursorCodec::decode),
		limit = limit,
	)

	private fun enforceRiskyLimit(adminUserId: UUID, response: HttpServletResponse) {
		val decision = rateLimit.enforce(
			scope = "admin:risky",
			subject = adminUserId.toString(),
			limit = properties.admin.rateLimit.riskyPerMinute,
			window = Duration.ofMinutes(1),
		)
		decision.budgetHeaders().forEach { name, values ->
			values.firstOrNull()?.let { response.setHeader(name, it) }
		}
	}
}
