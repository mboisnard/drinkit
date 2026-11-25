package com.drinkit.security

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.time.Instant

/**
 * The passkeys of the signed-in user, for an application that lists them in its API. Spring Security's URLs create
 * them, remove them and sign in with them. No bean while passkeys are off.
 */
@TechStarterTool
interface Passkeys {

    fun ofCurrentUser(): List<Passkey>
}

/** [id] in base64url, as the URL that removes it takes it. */
@TechStarterTool
data class Passkey(val id: String, val label: String, val created: Instant, val lastUsed: Instant)
