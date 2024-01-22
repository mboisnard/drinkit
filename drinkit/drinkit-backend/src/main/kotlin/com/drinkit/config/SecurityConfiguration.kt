package com.drinkit.config

import com.drinkit.user.security.JooqUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@EnableWebSecurity
class SecurityConfiguration {

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
                it.requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/**").hasAnyRole("USER")
                    .requestMatchers("/login/**").permitAll()
                    .requestMatchers("/actuator/**").hasRole("USER")
                    .requestMatchers("/openapi/**").hasRole("USER")
                    .anyRequest().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.ALWAYS).maximumSessions(1) }
            //.headers { it.frameOptions { it.sameOrigin() } }
            .formLogin(Customizer.withDefaults())

        return http.build()
    }

    @Bean
    fun authenticationProvider(jooqUserDetailsService: JooqUserDetailsService): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(jooqUserDetailsService)
        provider.setPasswordEncoder(BCryptPasswordEncoder())
        return provider
    }
}