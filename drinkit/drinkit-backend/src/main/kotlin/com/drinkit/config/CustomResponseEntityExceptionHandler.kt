package com.drinkit.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
internal class CustomResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    private val log = KotlinLogging.logger { }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleAccessDeniedException(ex: Exception): ResponseEntity<Any> {
        log.warn { ex.message }

        return ResponseEntity.badRequest().build()
    }
}
