package com.drinkit.i18n.infra

import com.drinkit.i18n.CurrentLocale
import com.drinkit.i18n.Translations
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(prefix = "starters.i18n", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(I18nProperties::class)
internal class I18nAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun translations(properties: I18nProperties): Translations = JsonCatalog(properties.catalog, properties.prefixes)

    @Bean
    @ConditionalOnMissingBean
    fun currentLocale(translations: Translations): CurrentLocale = RequestCurrentLocale(translations)
}
