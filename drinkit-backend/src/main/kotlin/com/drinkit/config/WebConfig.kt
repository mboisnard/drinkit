package com.drinkit.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// TODO: Remove this configuration for production
// WebMvcConfigurer overrides the converter bean definition in ObjectMapperConfiguration
@Configuration
@EnableWebMvc
class WebConfig(
    private val jacksonConverter: MappingJackson2HttpMessageConverter,
): WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedMethods("GET", "POST", "DELETE", "HEAD", "PUT")
            .allowedOrigins("*")
    }

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(jacksonConverter)
    }
}