package org.proxyma.backend.user

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

enum class UserStatus(val value: String) {
	ACTIVE("active"),
	SUSPENDED("suspended");

	companion object {
		fun fromValue(value: String): UserStatus =
			entries.firstOrNull { it.value == value }
				?: throw IllegalArgumentException("Unknown user status: $value")
	}
}

@Converter
class UserStatusConverter : AttributeConverter<UserStatus, String> {
	override fun convertToDatabaseColumn(attribute: UserStatus): String = attribute.value
	override fun convertToEntityAttribute(dbData: String): UserStatus = UserStatus.fromValue(dbData)
}

@Entity
@Table(name = "users")
class User(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(nullable = false, columnDefinition = "citext")
	var email: String,

	@Column(name = "password_hash", nullable = false, columnDefinition = "text")
	var passwordHash: String,

	@Column(name = "display_name", nullable = false, columnDefinition = "text")
	var displayName: String,

	@Column(name = "is_platform_admin", nullable = false)
	var platformAdmin: Boolean = false,

	@Convert(converter = UserStatusConverter::class)
	@Column(nullable = false, columnDefinition = "text")
	var status: UserStatus = UserStatus.ACTIVE,

	@Column(name = "suspended_at")
	var suspendedAt: Instant? = null,

	@Column(name = "suspended_by_user_id")
	var suspendedByUserId: UUID? = null,

	@Column(name = "suspension_reason", columnDefinition = "text")
	var suspensionReason: String? = null,

	@Column(name = "last_login_at")
	var lastLoginAt: Instant? = null,

	/**
	 * Embedded in every access token as the `tv` claim. Bumping it retires all
	 * access tokens issued before the change.
	 */
	@Column(name = "token_version", nullable = false)
	var tokenVersion: Int = 0,

	@CreationTimestamp
	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.now(),
)
