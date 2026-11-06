package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool

@TechStarterTool
interface Sessions {

    fun revokeAll(userId: String)
}
