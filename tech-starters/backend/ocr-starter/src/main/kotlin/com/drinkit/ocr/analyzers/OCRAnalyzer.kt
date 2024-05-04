package com.drinkit.ocr.analyzers

import com.drinkit.ocr.OCRResponse
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import java.util.*

enum class AnalyzerStatus {
    RUNNING,
    DISABLED,
}

const val EXECUTION_ORDER_HIGHER_LEVEL = 999

internal fun interface OCRAnalyzer {

    fun extractTextFrom(resource: Resource, locale: Locale): OCRResponse
}

internal data class OCRAnalyzers(
    val allAnalyzers: Map<OCRAvailableAnalyzer, AnalyzerStatus>,
    val instances: Map<OCRAvailableAnalyzer, OCRAnalyzer>,
) {
    val allRunningByExecutionOrder = instances.entries.sortedBy { it.key.executionOrder }.map { it.value }

    fun firstRunning() = allRunningByExecutionOrder.firstOrNull() ?: error("No OCR analyzer found")
}

enum class OCRAvailableAnalyzer(
    val computeStatus: (Environment) -> AnalyzerStatus,
    val executionOrder: Int,
) {
    GOOGLE_CLOUD_VISION(
        computeStatus = { env ->
            val cloudVisionEnabled = env.getProperty("ocr.cloud.google.cloud-vision.enabled", "false").toBoolean()

            if (!cloudVisionEnabled) {
                AnalyzerStatus.DISABLED
            } else {
                AnalyzerStatus.RUNNING
            }
        },
        executionOrder = 1,
    ),

    TESSERACT(
        computeStatus = { env ->
            try {
                val tesseractEnabled = env.getProperty("ocr.local.tesseract.enabled", "false").toBoolean()

                if (!tesseractEnabled) {
                    AnalyzerStatus.DISABLED
                } else {
                    val tessApi = net.sourceforge.tess4j.util.LoadLibs.getTessAPIInstance()

                    if (tessApi.TessVersion() != null) {
                        AnalyzerStatus.RUNNING
                    } else {
                        AnalyzerStatus.DISABLED
                    }
                }
            } catch (_: UnsatisfiedLinkError) {
                AnalyzerStatus.DISABLED
            }
        },
        executionOrder = EXECUTION_ORDER_HIGHER_LEVEL,
    ),
}
