package com.drinkit.featuremanagement.infra.openfeature

import com.drinkit.featuremanagement.Experiments
import com.drinkit.featuremanagement.FeatureFlags
import com.drinkit.kotlin.sha256
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.body
import tools.jackson.databind.json.JsonMapper

internal const val OFREP_BULK_PATH = "/ofrep/v1/evaluate/flags"
internal const val EXPOSURES_PATH = "/experiments/{key}/exposures"

private val KEY = Regex("^[a-z0-9][a-z0-9-]{0,99}$")
private const val MAX_KEYS = 50
private const val MAX_VARIANT_LENGTH = 64
private const val ETAG_HEX_LENGTH = 32

internal class OfrepApi(
    private val flags: FeatureFlags,
    private val experiments: Experiments,
    private val jsonMapper: JsonMapper,
) {

    fun evaluate(request: ServerRequest): ServerResponse {
        val asked = request.body<OfrepRequest>().context

        val evaluated = asked.flagKeys.map {
            OfrepFlag(it, flags.isEnabled(it))
        } + asked.experimentKeys.map {
            val assignment = experiments.assign(it)
            OfrepFlag(
                it,
                assignment.variant,
                if (assignment.inExperiment) "SPLIT" else "DEFAULT",
                assignment.variant,
            )
        }
        val body = jsonMapper.writeValueAsString(OfrepResponse(evaluated))
        val etag = "\"${body.sha256().take(ETAG_HEX_LENGTH)}\""

        val notModified = etag in request.headers().header(HttpHeaders.IF_NONE_MATCH)
        val response = (if (notModified) ServerResponse.status(HttpStatus.NOT_MODIFIED) else ServerResponse.ok())
            .header(HttpHeaders.ETAG, etag)
            .cacheControl(CacheControl.noStore().cachePrivate())

        return if (notModified) response.build() else response.contentType(MediaType.APPLICATION_JSON).body(body)
    }

    // The variant is assigned again, so that a forged or outdated report counts nothing
    fun reportExposure(request: ServerRequest): ServerResponse {
        val experiment = request.pathVariable("key").takeIf(KEY::matches)
            ?: return ServerResponse.badRequest().build()
        val reported = request.body<ExposureReport>()

        val assignment = experiments.assign(experiment)
        if (assignment.variant == reported.variant) experiments.trackExposure(assignment)

        return ServerResponse.noContent().build()
    }
}

internal data class OfrepRequest(val context: AskedKeys = AskedKeys())

internal data class AskedKeys(val flags: String = "", val experiments: String = "") {

    val flagKeys = keysOf(flags)
    val experimentKeys = keysOf(experiments)

    init {
        require(flagKeys.size + experimentKeys.size <= MAX_KEYS) { "At most $MAX_KEYS keys" }
        require((flagKeys + experimentKeys).all(KEY::matches)) { "A key is kebab case, 100 characters at most" }
    }
}

private fun keysOf(keys: String) = keys.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty) }.toSet()

internal data class OfrepResponse(val flags: List<OfrepFlag>)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class OfrepFlag(val key: String, val value: Any, val reason: String? = null, val variant: String? = null)

internal data class ExposureReport(val variant: String) {
    init {
        require(variant.length <= MAX_VARIANT_LENGTH) { "A variant holds $MAX_VARIANT_LENGTH characters at most" }
    }
}
