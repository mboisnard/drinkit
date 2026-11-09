package com.drinkit.featuremanagement.infra

import com.drinkit.featuremanagement.FlagSubject
import com.drinkit.featuremanagement.infra.openfeature.OpenFeatureFeatureFlags
import com.drinkit.security.InMemoryCurrentUser
import com.drinkit.security.InMemoryUserAccounts
import com.drinkit.security.UserAccount
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.providers.memory.Flag
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RequestSubjectTest {

    private val accounts = InMemoryUserAccounts()
    private val currentUser = InMemoryCurrentUser(accounts)
    private val subject = RequestSubject(currentUser, { "visitor-1" }, { mapOf("plan" to "vip") })

    @Test
    fun `opens a flag to admins and to a plan, from the roles and the attributes of the signed-in user`() {
        // Given
        val flags = OpenFeatureFeatureFlags(
            flagsTargeting(
                "admin-tools" to { context -> context.getValue("ROLE_ADMIN")?.asString() == "true" },
                "vip-cellar" to { context -> context.getValue("plan")?.asString() == "vip" },
            ),
            subject::get,
        )

        accounts.save("ada@drinkit.com", UserAccount("ada", setOf("ROLE_USER", "ROLE_ADMIN")))
        currentUser.signIn("ada")

        // When
        val opened = listOf("admin-tools", "vip-cellar").filter { flags.isEnabled(it) }

        // Then
        opened shouldBe listOf("admin-tools", "vip-cellar")
    }

    @Test
    fun `evaluates someone not signed in as their visitor, with no attribute`() {
        // When
        val evaluated = subject.get()

        // Then
        evaluated shouldBe FlagSubject("visitor-1")
    }

    private fun flagsTargeting(vararg rules: Pair<String, (EvaluationContext) -> Boolean>) =
        OpenFeatureAPI.createIsolated().apply {
            setProviderAndWait(
                InMemoryProvider(
                    rules.associate { (key, rule) ->
                        key to Flag.builder<Boolean>()
                            .variant("on", true)
                            .variant("off", false)
                            .defaultVariant("off")
                            .contextEvaluator { _, context -> rule(context) }
                            .build()
                    },
                ),
            )
        }.client
}
