package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool

/** The sessions of any user, on every instance. */
@TechStarterTool
interface Sessions {

    /** Signs [userId] out everywhere: every session and every remember-me token. */
    fun revokeAll(userId: String)
}
