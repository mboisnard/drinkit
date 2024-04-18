package com.drinkit.ocr.tesseract

import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.util.*

internal class TesseractDocumentAnalyserTest {

    private val tesseractDocumentAnalyser = TesseractDocumentAnalyser()

    @Test
    fun `should extract text from image using Tesseract package`() {

        val file = ClassPathResource("vin.jpg").file
        tesseractDocumentAnalyser.extractTextFrom(file, Locale.FRENCH)
    }
}