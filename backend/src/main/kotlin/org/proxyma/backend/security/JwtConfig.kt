package org.proxyma.backend.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.proxyma.backend.config.ProxymaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig {

	@Bean
	fun jwtSigningKey(properties: ProxymaProperties): SecretKey {
		val secret = properties.jwt.secret
		require(secret.isNotBlank()) {
			"PROXYMA_JWT_SECRET is required. Generate one with: openssl rand -base64 32"
		}
		val bytes = runCatching { Base64.getDecoder().decode(secret) }.getOrElse { secret.toByteArray() }
		require(bytes.size >= 32) {
			"PROXYMA_JWT_SECRET must contain at least 32 bytes (256 bits)."
		}
		return SecretKeySpec(bytes, "HmacSHA256")
	}

	@Bean
	fun jwtEncoder(signingKey: SecretKey): JwtEncoder {
		val source = ImmutableSecret<SecurityContext>(signingKey)
		return NimbusJwtEncoder(source)
	}

	@Bean
	fun jwtDecoder(signingKey: SecretKey, properties: ProxymaProperties): JwtDecoder {
		val decoder = NimbusJwtDecoder.withSecretKey(signingKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build()
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt.issuer))
		return decoder
	}
}
