package com.drinkit.ocr.tesseract

import com.drinkit.ocr.ExtractedText
import com.drinkit.ocr.OCRDocumentAnalyser
import io.github.oshai.kotlinlogging.KotlinLogging
import net.sourceforge.tess4j.Tesseract
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

enum class SupportedModelLanguage(val modelName: String) {
    ENGLISH("eng"),
    FRENCH("fra"),
}

@Service
internal class TesseractDocumentAnalyser : OCRDocumentAnalyser {

    private val logger = KotlinLogging.logger { }

    override fun extractTextFrom(file: File, locale: Locale): ExtractedText {

        val modelLanguage = locale.toModelLanguage()

        val tesseract = Tesseract()

        tesseract.setLanguage(modelLanguage.modelName)

        tesseract.setPageSegMode(PAGE_SEGMENTATION_MODE)
        tesseract.setOcrEngineMode(OCR_ENGINE_MODE)
        tesseract.setDatapath(MODELS_PATH)

        val extractedText = tesseract.doOCR(file)

        val sanitizedText = sanitizeOcrExtractedText(extractedText)

        return ExtractedText(extractedText, sanitizedText)
    }

    private fun sanitizeOcrExtractedText(rawText: String): String {
        return rawText.lines()
            .map { line -> line.trim().replace(Regex("[^A-Za-z0-9- ]"), "") }
            .filter { it.length > 2 }
            .joinToString("\n")
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
        private const val PAGE_SEGMENTATION_MODE = 4
        private const val MODELS_PATH = "src/main/resources/tesseract/tessdata"
        private const val OCR_ENGINE_MODE = 3
    }
}
