package org.proxyma.backend.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
	fun findByEmail(email: String): User?
	fun existsByEmail(email: String): Boolean

	/**
	 * Retires every access token issued for the account without touching its
	 * refresh tokens. Used when refresh-token reuse is detected: the family is
	 * revoked separately, while other devices can simply refresh to pick up the
	 * new version.
	 */
	@Modifying
	@Query("update User u set u.tokenVersion = u.tokenVersion + 1 where u.id = :userId")
	fun bumpTokenVersion(@Param("userId") userId: UUID): Int
}
