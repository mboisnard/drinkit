package com.drinkit.i18n

import com.drinkit.i18n.infra.JsonCatalog

/** The starter's [Translations], read from the catalog at [location] under [prefixes]. */
fun catalogTranslations(prefixes: Set<String>, location: String = "classpath*:translations/*.json"): Translations =
    JsonCatalog(location, prefixes)
