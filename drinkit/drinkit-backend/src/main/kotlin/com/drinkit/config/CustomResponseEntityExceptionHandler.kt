package com.drinkit.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
internal class CustomResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    // Hides the commons-logging logger of ResponseEntityExceptionHandler, which keeps using its own
    @Suppress("PROPERTY_HIDES_JAVA_FIELD")
    private val logger = KotlinLogging.logger { }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleAccessDeniedException(ex: Exception): ResponseEntity<Any> {
        logger.warn { ex.message }

        return ResponseEntity.badRequest().build()
    }
}
