package com.drinkit.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    @Value("\${server.servlet.session.cookie.name}")
    private val sessionCookieName: String
) {

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .cors {
                val configuration = CorsConfiguration()
                configuration.allowedOrigins = listOf("*")
                configuration.allowedMethods = listOf("GET", "POST", "DELETE", "HEAD", "PUT")

                val source = UrlBasedCorsConfigurationSource()
                source.registerCorsConfiguration("/**", configuration)

                it.configurationSource(source)
            }
            .authorizeHttpRequests {
                it.requestMatchers("/api/**").hasAnyRole("USER")
                    .requestMatchers("/login/**").permitAll()
                    .requestMatchers("/actuator/**").hasRole("USER")
                    .requestMatchers("/openapi/**").hasRole("USER")
                    .anyRequest().denyAll()
            }
            .httpBasic(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.ALWAYS).maximumSessions(1) }
            //.headers { it.frameOptions { it.sameOrigin() } }
            .formLogin(Customizer.withDefaults())
            .logout {
                it.logoutUrl("/api/logout")
                    .permitAll()
                    .logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                    .deleteCookies(sessionCookieName)
            }

        return http.build()
    }

    @Bean
    fun authenticationProvider(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder,
    ): AuthenticationProvider {

        val provider = DaoAuthenticationProvider()

        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)

        return provider
    }
}