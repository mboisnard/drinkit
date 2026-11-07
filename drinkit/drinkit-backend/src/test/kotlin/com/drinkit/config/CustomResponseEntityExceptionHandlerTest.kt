package com.drinkit.config

import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

internal class CustomResponseEntityExceptionHandlerTest {

    private val mockMvc = MockMvcBuilders.standaloneSetup(RefusingController())
        .setControllerAdvice(CustomResponseEntityExceptionHandler())
        .build()

    @Test
    fun `answers a refused argument with a problem holding the trace id of the request`() {
        MDC.putCloseable("traceId", "4bf92f3577b34da6a3ce929d0e0e4736").use {
            mockMvc.get("/refused").andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
                jsonPath("$.status") { value(400) }
                jsonPath("$.traceId") { value("4bf92f3577b34da6a3ce929d0e0e4736") }
            }
        }
    }

    @Test
    fun `adds the trace id to the problems Spring answers on its own`() {
        MDC.putCloseable("traceId", "4bf92f3577b34da6a3ce929d0e0e4736").use {
            mockMvc.get("/refused?count=not-a-number").andExpect {
                status { isBadRequest() }
                jsonPath("$.traceId") { value("4bf92f3577b34da6a3ce929d0e0e4736") }
            }
        }
    }

    @RestController
    private class RefusingController {
        @GetMapping("/refused")
        fun refused(count: Int?): String = "${requireNotNull(count) { "refused" }}"
    }
}
