package com.drinkit.ocr.analyzers

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.util.*

@Disabled
internal class TesseractDocumentAnalyserTest {

    private val tesseractDocumentAnalyser = TesseractAnalyzer()

    @Test
    fun `should extract text from image using Tesseract package`() {
        val file = ClassPathResource("vin.jpg")
        tesseractDocumentAnalyser.extractTextFrom(file, Locale.FRENCH)
    }
}
