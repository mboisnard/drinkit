package com.drinkit.security.filter

import com.drinkit.configuration.Configurations
import com.drinkit.configuration.getOrSetDefault
import com.drinkit.security.configuration.SecurityConfiguration
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter that blocks all traffic when maintenance mode is enabled.
 *
 * This filter checks the SecurityConfiguration.MaintenanceMode configuration.
 * If enabled, it returns a 503 Service Unavailable response for all requests.
 */
//@Component
class MaintenanceModeFilter(
    private val configurations: Configurations
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val maintenanceMode = configurations.getOrSetDefault(SecurityConfiguration.MaintenanceMode, false)

        if (maintenanceMode) {
            response.status = HttpStatus.SERVICE_UNAVAILABLE.value()
            response.writer.write("Application is currently in maintenance mode. Please try again later.")
            return
        }

        filterChain.doFilter(request, response)
    }
}
