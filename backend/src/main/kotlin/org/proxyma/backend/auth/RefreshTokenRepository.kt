package org.proxyma.backend.auth

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
	fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): RefreshToken?

	@Modifying
	@Query("update RefreshToken t set t.revokedAt = :revokedAt where t.familyId = :familyId and t.revokedAt is null")
	fun revokeFamily(@Param("familyId") familyId: UUID, @Param("revokedAt") revokedAt: Instant): Int

	@Modifying
	@Query("update RefreshToken t set t.revokedAt = :revokedAt where t.userId = :userId and t.revokedAt is null")
	fun revokeAllForUser(@Param("userId") userId: UUID, @Param("revokedAt") revokedAt: Instant): Int
}
