package com.drinkit.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive.COOKIES


@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity, securityContextRepository: SecurityContextRepository): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                    .requestMatchers("/api/**").hasAnyRole("USER")
                    .requestMatchers("/actuator/**").hasRole("USER")
                    .requestMatchers("/openapi/**").hasRole("USER")
                    .anyRequest().denyAll()
            }
            .securityContext {
                it.securityContextRepository(securityContextRepository)
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.ALWAYS).maximumSessions(1) }
            //.headers { it.frameOptions { it.sameOrigin() } }
            .logout {
                it.logoutUrl("/api/auth/logout")
                    .permitAll()
                    .logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                    .addLogoutHandler(HeaderWriterLogoutHandler(ClearSiteDataHeaderWriter(COOKIES)))
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

    @Bean
    @Throws(java.lang.Exception::class)
    fun authenticationManager(
        configuration: AuthenticationConfiguration
    ): AuthenticationManager = configuration.authenticationManager

    @Bean
    fun sessionContextRepository(): SecurityContextRepository = HttpSessionSecurityContextRepository()
}