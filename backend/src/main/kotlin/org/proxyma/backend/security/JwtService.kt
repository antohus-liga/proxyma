package org.proxyma.backend.security

import org.proxyma.backend.config.ProxymaProperties
import org.proxyma.backend.user.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.UUID

@Service
class JwtService(
	private val encoder: JwtEncoder,
	private val properties: ProxymaProperties,
	private val clock: Clock,
) {

	data class IssuedToken(val value: String, val expiresInSeconds: Long)

	fun issueAccessToken(user: User): IssuedToken {
		val now = clock.instant()
		val ttl = if (user.platformAdmin) {
			properties.auth.adminAccessTokenTtl
		} else {
			properties.auth.accessTokenTtl
		}
		val claims = JwtClaimsSet.builder()
			.issuer(properties.jwt.issuer)
			.subject(user.id.toString())
			.issuedAt(now)
			.expiresAt(now.plus(ttl))
			.id(UUID.randomUUID().toString())
			.build()
		val header = JwsHeader.with(MacAlgorithm.HS256).build()
		val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
		return IssuedToken(token, ttl.seconds)
	}
}
