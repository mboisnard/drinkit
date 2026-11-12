package com.drinkit.ai.infra.guardrails

import com.drinkit.ai.guardrails.AfterCallGuardrail
import com.drinkit.ai.guardrails.BeforeCallGuardrail
import com.drinkit.ai.infra.guardrails.after.FinishGuardrail
import com.drinkit.ai.infra.guardrails.after.LeakGuardrail
import com.drinkit.ai.infra.guardrails.after.OutputGuardrail
import com.drinkit.ai.infra.guardrails.after.PiiGuardrail
import com.drinkit.ai.infra.guardrails.before.InjectionGuardrail
import com.drinkit.ai.infra.guardrails.before.ObfuscationGuardrail
import com.drinkit.ai.infra.guardrails.before.SecretGuardrail
import io.micrometer.observation.ObservationFilter
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/** Each group backs off when the application declares a bean of its name, such as `injectionGuardrail`. */
@AutoConfiguration
@ConditionalOnBooleanProperty("starters.ai.guardrails.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GuardrailsProperties::class)
internal class GuardrailsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["aiGuardrails"])
    fun aiGuardrails(
        before: ObjectProvider<BeforeCallGuardrail>,
        after: ObjectProvider<AfterCallGuardrail>,
        properties: GuardrailsProperties,
    ) = ChatClientBuilderCustomizer { builder ->
        builder.defaultAdvisors(
            GuardrailsAdvisor(before.orderedStream().toList(), after.orderedStream().toList(), properties),
        )
    }

    @Bean
    @ConditionalOnMissingBean(name = ["aiGuardrailsObservationFilter"])
    fun aiGuardrailsObservationFilter(): ObservationFilter = GuardrailsObservationFilter()

    @Bean
    @ConditionalOnMissingBean(name = ["injectionGuardrail"])
    fun injectionGuardrail(): BeforeCallGuardrail = InjectionGuardrail()

    @Bean
    @ConditionalOnMissingBean(name = ["obfuscationGuardrail"])
    fun obfuscationGuardrail(): BeforeCallGuardrail = ObfuscationGuardrail()

    @Bean
    @ConditionalOnMissingBean(name = ["secretGuardrail"])
    fun secretGuardrail(): BeforeCallGuardrail = SecretGuardrail()

    @Bean
    @ConditionalOnMissingBean(name = ["outputGuardrail"])
    fun outputGuardrail(): AfterCallGuardrail = OutputGuardrail()

    @Bean
    @ConditionalOnMissingBean(name = ["piiGuardrail"])
    fun piiGuardrail(): AfterCallGuardrail = PiiGuardrail()

    @Bean
    @ConditionalOnMissingBean(name = ["leakGuardrail"])
    fun leakGuardrail(): AfterCallGuardrail = LeakGuardrail()

    @Bean
    @ConditionalOnMissingBean(name = ["finishGuardrail"])
    fun finishGuardrail(): AfterCallGuardrail = FinishGuardrail()
}
