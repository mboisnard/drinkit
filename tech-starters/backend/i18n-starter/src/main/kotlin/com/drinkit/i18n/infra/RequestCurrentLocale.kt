package com.drinkit.i18n.infra

import com.drinkit.i18n.CurrentLocale
import com.drinkit.i18n.Translations
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Locale

private const val SESSION_LANGUAGE = "starters.i18n.language"

/** Gives only a language of the catalog: any other the session or the browser names is never passed on. */
internal class RequestCurrentLocale(private val translations: Translations) : CurrentLocale {

    override fun get(): Locale = languageKeptForTheSession()
        ?: firstLanguageOfTheRequestTheCatalogHolds()
        ?: SOURCE_LANGUAGE

    override fun keepForTheSession(language: Locale) {
        // A text: the session stores its values as JSON, and reads back only texts and the types it allows
        request().getSession(true).setAttribute(SESSION_LANGUAGE, language.toLanguageTag())
    }

    private fun languageKeptForTheSession(): Locale? =
        (request().getSession(false)?.getAttribute(SESSION_LANGUAGE) as? String)
            ?.let(Locale::forLanguageTag)
            ?.takeIf { it in translations.languages }

    private fun firstLanguageOfTheRequestTheCatalogHolds(): Locale? {
        val acceptLanguage = request().getHeader(HttpHeaders.ACCEPT_LANGUAGE) ?: return null

        // A malformed header names no language
        val wanted = runCatching { Locale.LanguageRange.parse(acceptLanguage) }.getOrDefault(emptyList())
        return Locale.lookup(wanted, translations.languages)
    }

    private fun request(): HttpServletRequest =
        (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
}
