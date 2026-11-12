package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileAccess
import com.drinkit.file.storage.infra.FileStorageAutoConfiguration
import com.drinkit.security.InMemoryCurrentUser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.UserAccount
import com.drinkit.test.ControlledClock
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.DynamicPropertyRegistrar
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * The starter on a real database, with a clock the test moves, users it signs in, a private and a public usage, and
 * the members of event 42.
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(
    DataSourceAutoConfiguration::class,
    DataSourceTransactionManagerAutoConfiguration::class,
    TransactionAutoConfiguration::class,
    JooqAutoConfiguration::class,
    FileStorageAutoConfiguration::class,
)
internal class FileStorageTestContext {

    @Bean
    fun clock() = ControlledClock().fix(Instant.parse("2026-11-13T10:00:00Z"))

    @Bean
    fun currentUser(): InMemoryCurrentUser {
        val users = listOf(ADA, GRACE).map { "$it@example.com" to UserAccount(it, setOf("ROLE_USER")) }
        val admin = "$ADMIN@example.com" to UserAccount(ADMIN, setOf("ROLE_USER", "ROLE_ADMIN"))

        return InMemoryCurrentUser(InMemoryUserAccounts(*users.toTypedArray(), admin))
    }

    @Bean
    fun eventMembers() = EventMembers()

    /** A directory per context: each test class gets its own, since it dirties its context. */
    @Bean
    fun filesDirectory(): Path = Files.createTempDirectory("stored-files")

    @Bean
    fun fileStorageSettings(filesDirectory: Path) = DynamicPropertyRegistrar { properties ->
        properties.add("starters.file-storage.filesystem.directory") { filesDirectory.toString() }
        properties.add("starters.file-storage.usages.$PRIVATE_USAGE.types") { "image/jpeg" }
        properties.add("starters.file-storage.usages.$PUBLIC_USAGE.types") { "image/jpeg" }
        properties.add("starters.file-storage.usages.$PUBLIC_USAGE.visibility") { "public" }
    }

    /** Grace is the only other member of event 42, and counts the questions she answers. */
    class EventMembers : FileAccess {
        var questions = 0

        override val resourceType = "event"

        override fun canRead(resourceId: String, readerId: String?): Boolean {
            questions++

            return resourceId == "42" && readerId == GRACE
        }

        override fun canDelete(resourceId: String, userId: String?) = resourceId == "42" && userId == GRACE
    }

    companion object {
        const val ADA = "ada"
        const val GRACE = "grace"
        const val ADMIN = "admin"
        const val PRIVATE_USAGE = "bottle-photo"
        const val PUBLIC_USAGE = "avatar"
    }
}
