package com.drinkit.featuremanagement.infra

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "starters.feature-management")
internal data class FeatureManagementProperties(val enabled: Boolean = true, val flipt: Flipt = Flipt()) {

    data class Flipt(val url: String = "", val namespace: String = "default", val environment: String = "")
}
