package com.drinkit.i18n.infra

import com.drinkit.i18n.Translations
import com.ibm.icu.text.MessageFormat
import com.ibm.icu.util.ULocale
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import java.util.Locale

/** One flat JSON file of ICU messages per language, named after it, such as `fr.json`. */
internal class JsonCatalog(location: String, prefixes: Set<String>) : Translations {

    // Only the keys of the prefixes, as each Nuxt app loads only its own: the catalog holds the texts of every app
    private val messages: Map<Locale, Map<String, String>> = PathMatchingResourcePatternResolver()
        .getResources(location)
        .associate { file ->
            val language = Locale.forLanguageTag(file.filename.orEmpty().removeSuffix(".json"))
            val all = file.inputStream.use { JsonMapper().readValue(it, MESSAGES) }

            language to all.filterKeys { it.substringBefore('.') in prefixes }
        }

    override val languages: Set<Locale> = messages.keys

    init {
        require(prefixes.isNotEmpty()) { "No prefix declared in starters.i18n.prefixes: the catalog would load no key" }
        require(SOURCE_LANGUAGE in languages) { "No ${SOURCE_LANGUAGE.language}.json found at $location" }
    }

    override fun translate(key: String, locale: Locale, values: Map<String, Any>): String {
        val language = languages.firstOrNull { it.language == locale.language } ?: SOURCE_LANGUAGE
        val pattern = requireNotNull(messages.getValue(language)[key]) { "The catalog lacks $key in $language" }

        // Built for each call: an ICU MessageFormat is not thread-safe
        return MessageFormat(pattern, ULocale.forLocale(language)).format(values)
    }

    private companion object {
        val MESSAGES = object : TypeReference<Map<String, String>>() {}
    }
}

internal val SOURCE_LANGUAGE: Locale = Locale.ENGLISH
