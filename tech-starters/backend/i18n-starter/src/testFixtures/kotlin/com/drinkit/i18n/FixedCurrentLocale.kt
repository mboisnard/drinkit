package com.drinkit.i18n

import java.util.Locale

class FixedCurrentLocale(private var locale: Locale = Locale.ENGLISH) : CurrentLocale {

    override fun get() = locale

    override fun keepForTheSession(language: Locale) {
        locale = language
    }
}
