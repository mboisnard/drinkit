package com.drinkit.user.core

import java.util.Locale

data class Preferences(val language: Locale)

sealed interface PreferenceChange {
    fun applyTo(preferences: Preferences): Preferences
}

data class ChooseLanguage(val language: Locale) : PreferenceChange {
    override fun applyTo(preferences: Preferences) = preferences.copy(language = language)
}
