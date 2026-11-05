package com.drinkit.security.infra.session

import com.drinkit.security.SessionData
import com.drinkit.security.sessionKey
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

internal data class Preferences(val currency: String, val favorites: List<String>)

private val PREFERENCES = sessionKey<Preferences>("preferences")
private val RECENT_SEARCHES = sessionKey<List<Preferences>>("recent-searches")

@RestController
internal class SessionsTestController(private val sessionData: SessionData) {

    @PutMapping("/api/public/preferences")
    fun storePreferences(): ResponseEntity<Unit> {
        sessionData[PREFERENCES] = Preferences("EUR", listOf("Margaux", "Pomerol"))
        sessionData[RECENT_SEARCHES] = listOf(Preferences("USD", listOf("Napa")))
        return ResponseEntity.noContent().build()
    }

    // Reads each value through its type: a value read back as a map would fail here
    @GetMapping("/api/public/preferences")
    fun preferences() = listOfNotNull(
        sessionData[PREFERENCES]?.favorites?.joinToString(),
        sessionData[RECENT_SEARCHES]?.single()?.currency,
    )
}
