package com.drinkit.security.configuration

import com.drinkit.configuration.ConfigurationKey

/**
 * Security-related configuration keys.
 */
object SecurityConfiguration {

    /**
     * Maintenance mode toggle.
     * When enabled, all HTTP requests are blocked.
     */
    object MaintenanceMode : ConfigurationKey<Boolean>
}
