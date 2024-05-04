package com.drinkit.ocr

import com.drinkit.ocr.analyzers.OCRAnalyzers
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.util.*

sealed interface OCRResponse

data class ExtractedText(
    val value: String,
) : OCRResponse

data class Error(val reason: String) : OCRResponse

interface OCRAnalysis {

    fun featureAvailable(): Boolean

    fun ocrInformation(): Map<String, String>

    fun extractText(resource: Resource, locale: Locale): OCRResponse
}

@Service
internal class InternalOCRAnalysis(
    private val ocrAnalyzers: OCRAnalyzers
) : OCRAnalysis {

    override fun featureAvailable(): Boolean = ocrAnalyzers.instances.isNotEmpty()

    override fun ocrInformation(): Map<String, String> = ocrAnalyzers
        .allAnalyzers.entries
        .associate { it.key.name to it.value.name }

    override fun extractText(resource: Resource, locale: Locale): OCRResponse {
        val analyzer = ocrAnalyzers.firstRunning()
        return analyzer.extractTextFrom(resource, locale)
    }
}
