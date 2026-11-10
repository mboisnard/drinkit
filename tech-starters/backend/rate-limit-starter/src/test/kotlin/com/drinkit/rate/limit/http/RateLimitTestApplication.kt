package com.drinkit.rate.limit.http

import com.drinkit.rate.limit.RateLimit
import com.drinkit.rate.limit.RateLimitPolicy
import com.drinkit.rate.limit.RateLimitRule
import com.drinkit.rate.limit.RateLimited
import com.drinkit.rate.limit.RateLimitedCall
import com.drinkit.rate.limit.RateLimiter
import com.drinkit.test.ControlledClock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@SpringBootApplication
internal class RateLimitTestApplication {

    @Bean
    fun clock() = ControlledClock()
}

@Component
internal class CatalogLimit : RateLimitPolicy {
    override val rule = RateLimitRule("catalog", listOf(RateLimit(2, Duration.ofHours(1))))

    override fun key(call: RateLimitedCall) = "ada"
}

@Component
internal class CodeLimit : RateLimitPolicy {
    override val rule = RateLimitRule("codes", listOf(RateLimit(1, Duration.ofHours(1))))

    override fun key(call: RateLimitedCall) = call.argument<String>().lowercase()
}

/** Counts nobody, as a policy that lets an admin through would for an admin. */
@Component
internal class UncountedLimit : RateLimitPolicy {
    override val rule = RateLimitRule("reports", listOf(RateLimit(1, Duration.ofHours(1))))

    override fun key(call: RateLimitedCall): String? = null
}

@RestController
internal class LimitedController(private val jdbc: JdbcClient, private val rateLimiter: RateLimiter) {

    @GetMapping("/limited/catalog")
    @RateLimited(CatalogLimit::class)
    fun catalog(): ResponseEntity<Unit> = ResponseEntity.ok().build()

    @GetMapping("/limited/codes")
    @RateLimited(CodeLimit::class)
    fun codes(
        @RequestParam email: String,
    ) = "Code sent to $email"

    @GetMapping("/limited/reports")
    @RateLimited(UncountedLimit::class)
    fun reports(): ResponseEntity<Unit> = ResponseEntity.ok().build()

    @GetMapping("/limited/imports")
    @Transactional
    fun imports(): String {
        jdbc.sql("SELECT 1").query().singleValue()
        rateLimiter.consume(RateLimitRule("imports", listOf(RateLimit(1, Duration.ofHours(1)))), "ada").orThrow()

        throw ResponseStatusException(HttpStatus.CONFLICT, "The import failed")
    }
}
