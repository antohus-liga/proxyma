package org.proxyma.backend.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.InetAddress
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(name = "user_id", nullable = false)
	var userId: UUID,

	@Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "text")
	var tokenHash: String,

	@Column(name = "family_id", nullable = false)
	var familyId: UUID,

	@Column(name = "replaced_by_token_id")
	var replacedByTokenId: UUID? = null,

	@Column(name = "user_agent", columnDefinition = "text")
	var userAgent: String? = null,

	@JdbcTypeCode(SqlTypes.INET)
	@Column(name = "ip")
	var ip: InetAddress? = null,

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),

	@Column(name = "expires_at", nullable = false)
	var expiresAt: Instant,

	@Column(name = "revoked_at")
	var revokedAt: Instant? = null,
)
