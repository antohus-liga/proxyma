package org.proxyma.backend.security

import org.proxyma.backend.config.ProxymaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(
		http: HttpSecurity,
		entryPoint: RestAuthenticationEntryPoint,
		accessDeniedHandler: RestAccessDeniedHandler,
		userStatusFilter: UserStatusFilter,
		properties: ProxymaProperties,
	): SecurityFilterChain {
		http
			.csrf { it.disable() }
			.cors { it.configurationSource(corsConfigurationSource(properties)) }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests { authorize ->
				authorize.requestMatchers(
					HttpMethod.POST,
					"/api/v1/auth/register",
					"/api/v1/auth/login",
					"/api/v1/auth/refresh",
					"/api/v1/auth/logout",
				).permitAll()
				authorize.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				authorize.anyRequest().authenticated()
			}
			.oauth2ResourceServer { resourceServer ->
				resourceServer.authenticationEntryPoint(entryPoint)
				resourceServer.jwt { }
			}
			.exceptionHandling { exceptions ->
				exceptions.authenticationEntryPoint(entryPoint)
				exceptions.accessDeniedHandler(accessDeniedHandler)
			}
			.addFilterAfter(userStatusFilter, BearerTokenAuthenticationFilter::class.java)

		return http.build()
	}

	private fun corsConfigurationSource(properties: ProxymaProperties): CorsConfigurationSource {
		val configuration = CorsConfiguration()
		configuration.allowedOrigins = properties.cors.allowedOrigins
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
		configuration.allowedHeaders = listOf("*")
		configuration.allowCredentials = true
		configuration.maxAge = 3600

		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)
		return source
	}
}
