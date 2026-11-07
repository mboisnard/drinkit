package com.drinkit.sse

import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.endpointRules
import com.drinkit.test.ControlledClock
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
internal class SseTestApplication {

    @Bean
    fun clock() = ControlledClock()

    @Bean
    fun userAccounts() = InMemoryUserAccounts()

    @Bean
    fun signInCodeSender() = SpySignInCodeSender()

    @Bean
    fun visitorEndpoints() = endpointRules {
        authorize("/visit", permitAll)
    }
}

// A page of the application that opens the session of a visitor, for the stream to belong to it
@RestController
internal class VisitController {

    @GetMapping("/visit")
    fun visit(request: HttpServletRequest): ResponseEntity<Unit> {
        request.getSession(true)
        return ResponseEntity.noContent().build()
    }
}
