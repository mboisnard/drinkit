package com.drinkit.ocr.analyzers

import com.drinkit.ocr.ExtractedText
import com.drinkit.ocr.OCRResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode.OEM_DEFAULT
import net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_COLUMN
import net.sourceforge.tess4j.Tesseract
import org.springframework.core.io.Resource
import java.util.*

enum class SupportedModelLanguage(val modelName: String) {
    ENGLISH("eng"),
    FRENCH("fra"),
}

internal class TesseractAnalyzer : OCRAnalyzer {

    private val logger = KotlinLogging.logger { }

    override fun extractTextFrom(resource: Resource, locale: Locale): OCRResponse {
        val modelLanguage = locale.toModelLanguage()

        val tesseract = Tesseract()

        tesseract.setLanguage(modelLanguage.modelName)

        tesseract.setPageSegMode(PAGE_SEGMENTATION_MODE)
        tesseract.setOcrEngineMode(OCR_ENGINE_MODE)
        tesseract.setDatapath(MODELS_PATH)

        val text = tesseract.doOCR(resource.file)

        return ExtractedText(text)
    }

    private fun Locale.toModelLanguage(): SupportedModelLanguage =
        when (this.language) {
            "en" -> SupportedModelLanguage.ENGLISH
            "fr" -> SupportedModelLanguage.FRENCH
            else -> {
                logger.info { "Not supported locale for OCR Model, $this, defaulting to english model" }
                SupportedModelLanguage.ENGLISH
            }
        }

    companion object {
        // 1, 2, 3, 4, 11
        private const val PAGE_SEGMENTATION_MODE = PSM_SINGLE_COLUMN
        private const val MODELS_PATH = "src/main/resources/tesseract/tessdata"
        private const val OCR_ENGINE_MODE = OEM_DEFAULT
    }
}
