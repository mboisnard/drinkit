package com.drinkit.i18n.infra

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "starters.i18n")
internal data class I18nProperties(
    val enabled: Boolean = true,
    val catalog: String = "classpath*:translations/*.json",
    val prefixes: Set<String> = emptySet(),
)
