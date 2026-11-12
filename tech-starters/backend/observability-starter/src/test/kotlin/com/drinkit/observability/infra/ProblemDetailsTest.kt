package com.drinkit.observability.infra

import com.jayway.jsonpath.JsonPath
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@SpringBootTest(classes = [ProblemDetailsTest.Application::class])
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
internal class ProblemDetailsTest(private val mockMvc: MockMvc) {

    @Test
    fun `answers an exception carrying its status with a problem of that status holding the trace id`() {
        // When
        val response = mockMvc.get("/refused")

        // Then
        response.andExpect {
            status { isConflict() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(409) }
        }
        traceIdOf(response) shouldMatch TRACE_ID_FORMAT
    }

    @Test
    fun `answers an unexpected failure with a 500 problem that keeps the error message out`() {
        // When
        val response = mockMvc.get("/failing")

        // Then
        response.andExpect {
            status { isInternalServerError() }
        }
        traceIdOf(response) shouldMatch TRACE_ID_FORMAT
        response.andReturn().response.contentAsString shouldNotContain SECRET
    }

    @Test
    fun `answers the refusal a servlet filter hands to Spring MVC with the same problem`() {
        // When
        val response = mockMvc.get("/filtered") { accept = MediaType.APPLICATION_JSON }

        // Then
        response.andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
        traceIdOf(response) shouldMatch TRACE_ID_FORMAT
    }

    private fun traceIdOf(response: ResultActionsDsl): String =
        JsonPath.read(response.andReturn().response.contentAsString, "$.traceId")

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import(TestController::class)
    class Application {

        @Bean
        fun refusingFilter(
            @Lazy @Qualifier("handlerExceptionResolver") resolver: HandlerExceptionResolver,
        ) = FilterRegistrationBean(RefusingFilter(resolver))
    }

    @RestController
    class TestController {

        @GetMapping("/refused")
        fun refused(): String = throw ErrorResponseException(HttpStatus.CONFLICT)

        @GetMapping("/failing")
        fun failing(): String = error(SECRET)
    }

    class RefusingFilter(private val resolver: HandlerExceptionResolver) : OncePerRequestFilter() {

        override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
            if (request.requestURI == "/filtered") {
                resolver.resolveException(request, response, null, ErrorResponseException(HttpStatus.UNAUTHORIZED))
            } else {
                chain.doFilter(request, response)
            }
        }
    }

    private companion object {
        val TRACE_ID_FORMAT = Regex("[0-9a-f]{32}")
        const val SECRET = "connection string with a password"
    }
}
