package com.drinkit.i18n

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.util.Locale

/** The language of the catalog the current request reads in. Only inside a request. */
@TechStarterTool
interface CurrentLocale {

    /** The language kept for the session, else the first of `Accept-Language` the catalog holds, else English. */
    fun get(): Locale

    /**
     * Keeps [language] for the rest of the session, such as the account's at sign-in. [get] ignores it while the
     * catalog lacks it.
     */
    fun keepForTheSession(language: Locale)
}
