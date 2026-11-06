package com.drinkit.security

import com.drinkit.test.ControlledClock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

@SpringBootApplication
internal class SecurityTestApplication {

    @Bean
    fun clock() = ControlledClock()

    @Bean
    fun userAccounts() = InMemoryUserAccounts()

    @Bean
    fun signInCodeSender() = SpySignInCodeSender()

    @Bean
    @Order(1)
    fun publicEndpoints() = endpointRules {
        authorize("/api/public/**", permitAll)
    }

    @Bean
    @Order(2)
    fun userEndpoints() = endpointRules {
        authorize(HttpMethod.GET, "/api/cellars/{id}", hasRole("USER"))
        authorize("/api/cellars/**", hasRole("USER"))
        authorize("/api/admin/**", hasRole("USER"))
    }
}

@RestController
internal class TestController(private val currentUser: CurrentUser) {

    @GetMapping("/api/public/me")
    fun publicMe() = currentUser.get()

    @GetMapping("/api/cellars/me")
    fun me() = currentUser.get()

    @GetMapping("/api/cellars/{id}")
    fun cellar(
        @PathVariable id: String,
    ) = id

    @PostMapping("/api/cellars")
    fun createCellar() = ResponseEntity.status(201).build<Unit>()

    @PostMapping("/api/public/sign-in-as/{id}")
    fun signIn(
        @PathVariable id: String,
    ) = currentUser.signIn(id)

    @PostMapping("/api/cellars/refresh")
    fun refresh() = currentUser.refresh()

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/stats")
    fun adminStats() = currentUser.get().id

    @PreAuthorize("isFullyAuthenticated()")
    @GetMapping("/api/cellars/fully-signed-in")
    fun fullySignedIn() = ResponseEntity.noContent().build<Unit>()

    @PostMapping("/api/public/ping")
    fun ping() = ResponseEntity.noContent().build<Unit>()

    @GetMapping("/api/unruled")
    fun unruled() = ResponseEntity.noContent().build<Unit>()
}

@RestControllerAdvice
internal class CatchAllAdvice {

    @ExceptionHandler(Exception::class)
    fun anyError() = ResponseEntity.internalServerError().build<Unit>()
}
