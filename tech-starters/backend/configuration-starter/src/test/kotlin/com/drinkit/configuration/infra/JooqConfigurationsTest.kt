package com.drinkit.configuration.infra

import com.drinkit.configuration.Configurations
import com.drinkit.configuration.generated.jooq.DrinkitApplication
import com.drinkit.configuration.generated.jooq.tables.references.CONFIGURATION
import com.drinkit.jooq.JooqIntegrationTest
import com.drinkit.test.ControlledClock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL.currentOffsetDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Instant

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
@ExtendWith(OutputCaptureExtension::class)
internal class JooqConfigurationsTest : ConfigurationsTestContract() {

    private lateinit var dsl: DSLContext

    @BeforeEach
    fun setup(dsl: DSLContext) {
        this.dsl = dsl
    }

    private val clock = ControlledClock().fix(Instant.parse("2026-11-03T10:15:30Z"))

    override fun fetchConfigurations(): Configurations = JooqConfigurations(dsl, jacksonMapperBuilder().build(), clock)

    @Test
    fun `writes nothing when it reads a default`() {
        // When
        configurations[TestKeys.limits]

        // Then
        dsl.fetchCount(CONFIGURATION) shouldBe 0
    }

    @Test
    fun `stamps a value set with the time of the application clock`() {
        // When
        configurations[TestKeys.enabled] = true

        // Then
        dsl.select(CONFIGURATION.MODIFIED).from(CONFIGURATION).fetchSingle().value1()!!.toInstant() shouldBe
            clock.instant()
    }

    @Test
    fun `stores a text holding the null character that PostgreSQL refuses in JSONB`() {
        // When
        configurations[TestKeys.label] = "Pasted\u0000 text"

        // Then
        configurations[TestKeys.label] shouldBe "Pasted text"
    }

    @Test
    fun `fills a field missing from the stored value with its default`() {
        // Given
        store(TestKeys.limits.name, """{"maxItems": 42}""")

        // When
        val limits = configurations[TestKeys.limits]

        // Then
        limits shouldBe Limits(maxItems = 42, label = "none")
    }

    @Test
    fun `ignores a field of the stored value that the group no longer has`() {
        // Given
        store(TestKeys.limits.name, """{"maxItems": 42, "removed": true}""")

        // When
        val limits = configurations[TestKeys.limits]

        // Then
        limits shouldBe Limits(maxItems = 42)
    }

    @Test
    fun `reads the default when the stored value breaks the rule of its group`() {
        // Given
        store(TestKeys.limits.name, """{"maxItems": 1000}""")

        // When
        val limits = configurations[TestKeys.limits]

        // Then
        limits shouldBe Limits()
    }

    @Test
    fun `reads the default when the stored value has another type`() {
        // Given
        store(TestKeys.enabled.name, """"yes please"""")

        // When
        val enabled = configurations[TestKeys.enabled]

        // Then
        enabled shouldBe false
    }

    @Test
    fun `logs the key of a stored value it cannot read`(output: CapturedOutput) {
        // Given
        store(TestKeys.limits.name, """{"maxItems": 1000}""")

        // When
        configurations[TestKeys.limits]

        // Then
        output.out shouldContain "test.limits"
    }

    private fun store(name: String, json: String) {
        dsl.insertInto(CONFIGURATION)
            .set(CONFIGURATION.KEY, name)
            .set(CONFIGURATION.VALUE, JSONB.jsonb(json))
            .set(CONFIGURATION.MODIFIED, currentOffsetDateTime())
            .execute()
    }
}
