package org.proxyma.backend.security

import org.proxyma.backend.common.api.ProblemResponseWriter
import org.proxyma.backend.config.ProxymaProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class SecurityConfig {

	@Bean
	fun securityFilterChain(
		http: HttpSecurity,
		entryPoint: RestAuthenticationEntryPoint,
		accessDeniedHandler: RestAccessDeniedHandler,
		userStatusFilter: UserStatusFilter,
	): SecurityFilterChain {
		http
			.csrf { it.disable() }
			// Picks up the `corsFilter` bean below so rejections render problem+json.
			.cors { }
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

	@Bean
	fun corsFilter(
		properties: ProxymaProperties,
		writer: ProblemResponseWriter,
	): CorsFilter {
		val configuration = CorsConfiguration()
		configuration.allowedOrigins = properties.cors.allowedOrigins
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
		configuration.allowedHeaders = listOf("*")
		configuration.allowCredentials = true
		configuration.maxAge = 3600

		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)

		return CorsFilter(source).apply { setCorsProcessor(ProblemCorsProcessor(writer)) }
	}

	/**
	 * `CorsFilter` is a `Filter` bean, so Boot would also register it in the
	 * servlet container. It is only meant to run inside the security chain;
	 * registering it twice would add duplicate CORS handling.
	 */
	@Bean
	fun corsFilterRegistration(filter: CorsFilter): FilterRegistrationBean<CorsFilter> =
		FilterRegistrationBean(filter).apply { isEnabled = false }
}
