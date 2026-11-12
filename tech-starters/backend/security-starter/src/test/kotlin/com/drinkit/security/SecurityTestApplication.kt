package com.drinkit.security

import com.drinkit.rate.limit.inMemoryRateLimiter
import com.drinkit.test.ControlledClock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Clock

@SpringBootApplication
internal class SecurityTestApplication {

    @Bean
    fun clock() = ControlledClock()

    @Bean
    fun rateLimiter(clock: Clock) = inMemoryRateLimiter(clock)

    @Bean
    fun userAccounts() = InMemoryUserAccounts()

    @Bean
    fun signInCodeSender() = SpySignInCodeSender()

    @Bean
    @Order(1)
    fun publicEndpoints() = endpointRules(
        csrfFree = listOf(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/public/evaluate")),
    ) {
        authorize("/api/public/**", permitAll)
    }

    @Bean
    @Order(2)
    fun userEndpoints() = endpointRules {
        authorize(HttpMethod.GET, "/api/cellars/{id}", hasRole("USER"))
        authorize("/api/cellars/**", hasRole("USER"))
        authorize("/api/admin/**", hasRole("USER"))
    }

    @Bean
    @Order(3)
    fun actuatorEndpoints() = endpointRules {
        authorize("/actuator/**", hasRole("ADMIN"))
    }
}

@RestController
internal class TestController(private val currentUser: CurrentUser, private val currentVisitor: CurrentVisitor) {

    @GetMapping("/api/public/visitor")
    fun visitor() = currentVisitor.id()

    @GetMapping("/api/public/visitor/ip-address")
    fun visitorIpAddress() = currentVisitor.ipAddress().value

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

    @PostMapping("/api/public/evaluate")
    fun evaluate() = ResponseEntity.noContent().build<Unit>()

    @GetMapping("/api/unruled")
    fun unruled() = ResponseEntity.noContent().build<Unit>()
}

// What the observability starter gives an application: the refusals of the chain become problem details
@ControllerAdvice
@Order(0)
internal class ProblemDetailsAdvice : ResponseEntityExceptionHandler()

@RestControllerAdvice
internal class CatchAllAdvice {

    @ExceptionHandler(Exception::class)
    fun anyError() = ResponseEntity.internalServerError().build<Unit>()
}
