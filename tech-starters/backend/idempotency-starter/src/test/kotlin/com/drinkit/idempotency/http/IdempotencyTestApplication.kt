package com.drinkit.idempotency.http

import com.drinkit.idempotency.Idempotent
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.SpySignInCodeSender
import com.drinkit.security.endpointRules
import com.drinkit.test.ControlledClock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootApplication
internal class IdempotencyTestApplication {

    @Bean
    fun clock() = ControlledClock()

    @Bean
    fun userAccounts() = InMemoryUserAccounts()

    @Bean
    fun signInCodeSender() = SpySignInCodeSender()

    @Bean
    fun cellars(transactions: PlatformTransactionManager) = Cellars(transactions)

    @Bean
    fun cellarEndpoints() = endpointRules {
        authorize("/cellars/**", authenticated)
    }
}

/** Cellars kept as a database would: a creation counts once its transaction commits, and never if it rolls back. */
internal class Cellars(private val transactions: PlatformTransactionManager) {

    val created = CopyOnWriteArrayList<String>()

    /** What the next creations answer once the cellar is written, such as a server error. */
    val nextAnswers = ConcurrentLinkedQueue<ResponseEntity<String>>()

    /** The next creations their use case refuses, as a business rule would. */
    @Volatile
    var refused = false

    /** Holds the creations until [resume], for a retry to arrive while one runs. */
    @Volatile
    var pause: Pause? = null

    class Pause {
        val started = CountDownLatch(1)
        private val resumed = CountDownLatch(1)

        fun hold() {
            started.countDown()
            resumed.await(5, TimeUnit.SECONDS)
        }

        fun resume() = resumed.countDown()
    }

    fun create(name: String): String {
        val id = "cellar-${created.size + 1}"
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        created += name
                    }
                },
            )
        } else {
            created += name
        }

        return id
    }

    /** Fails in a transaction of the use case, which marks the request's own transaction to roll back. */
    fun refuse() {
        runCatching { TransactionTemplate(transactions).executeWithoutResult { error("A cellar needs a city") } }
    }
}

internal data class CreateCellarRequest(val name: String)

internal interface CellarsApi {
    fun createCellar(request: CreateCellarRequest): ResponseEntity<String>
}

// As the controllers of DrinkIt: they implement the interface generated from the API contract, behind a proxy
@RestController
@PreAuthorize("isAuthenticated()")
internal class CellarsController(private val cellars: Cellars) : CellarsApi {

    @Idempotent
    @PostMapping("/cellars")
    override fun createCellar(
        @RequestBody request: CreateCellarRequest,
    ): ResponseEntity<String> {
        cellars.pause?.hold()

        if (cellars.refused) {
            cellars.refuse()
            return ResponseEntity.badRequest().build()
        }

        val id = cellars.create(request.name)

        return cellars.nextAnswers.poll() ?: ResponseEntity.created(URI.create("/cellars/$id"))
            .header("Set-Cookie", ResponseCookie.from("last-cellar", id).build().toString())
            .body(id)
    }

    @PostMapping("/cellars/tasting")
    fun taste(
        @RequestBody request: CreateCellarRequest,
    ): ResponseEntity<String> {
        cellars.create(request.name)

        return ResponseEntity.ok("tasted")
    }
}
