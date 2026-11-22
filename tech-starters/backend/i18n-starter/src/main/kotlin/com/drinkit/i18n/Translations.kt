package com.drinkit.i18n

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.Locale

/** The messages of the catalog, in ICU MessageFormat, under the prefixes the application declares. */
@TechStarterTool
interface Translations {

    /** The languages of the catalog, English among them. */
    val languages: Set<Locale>

    /**
     * The message of [key] in the language of [locale], or in English when the catalog lacks that language, with
     * [values] placed by name. Fails on a key the catalog lacks.
     */
    fun translate(key: String, locale: Locale, values: Map<String, Any> = emptyMap()): String
}
