package org.proxyma.backend.security

import java.util.UUID

data class ProxymaPrincipal(
	val userId: UUID,
	val email: String,
	val displayName: String,
	val platformAdmin: Boolean,
)
