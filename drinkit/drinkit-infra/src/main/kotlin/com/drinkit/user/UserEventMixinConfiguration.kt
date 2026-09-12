package com.drinkit.user

import com.drinkit.common.Author
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.module.SimpleModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Author.Connected::class, name = "CONNECTED"),
    JsonSubTypes.Type(value = Author.Unlogged::class, name = "UNLOGGED")
)
internal abstract class AuthorMixin

@Configuration
internal class UserEventMixinConfiguration {

    @Bean
    fun authorModule(): JacksonModule {
        return SimpleModule()
            .setMixInAnnotation(Author::class.java, AuthorMixin::class.java)
    }
}