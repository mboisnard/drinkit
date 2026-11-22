package com.drinkit.i18n

import java.util.Locale

internal class FixedCurrentLocaleTest : CurrentLocaleTestContract() {

    override fun givenARequestIn(language: Locale): CurrentLocale = FixedCurrentLocale(language)
}
