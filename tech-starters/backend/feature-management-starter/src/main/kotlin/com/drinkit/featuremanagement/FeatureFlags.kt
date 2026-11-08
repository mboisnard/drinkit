package com.drinkit.featuremanagement

import com.drinkit.documentation.tech.starter.TechStarterTool

/** Without a subject, a flag is evaluated for the signed-in user or the visitor of the current request. */
@TechStarterTool
interface FeatureFlags {

    fun isEnabled(flag: String, default: Boolean = false): Boolean

    fun isEnabled(flag: String, subject: FlagSubject, default: Boolean = false): Boolean

    fun isDisabled(flag: String, default: Boolean = false): Boolean = !isEnabled(flag, default)

    fun isDisabled(flag: String, subject: FlagSubject, default: Boolean = false): Boolean =
        !isEnabled(flag, subject, default)
}
