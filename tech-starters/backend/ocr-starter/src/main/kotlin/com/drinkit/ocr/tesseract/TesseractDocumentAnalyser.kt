package com.drinkit.ocr.tesseract

import com.drinkit.ocr.ExtractedText
import com.drinkit.ocr.OCRDocumentAnalyser
import net.sourceforge.tess4j.ITesseract
import org.springframework.stereotype.Service
import java.io.File

@Service
internal class TesseractDocumentAnalyser(
    private val tesseract: ITesseract,
) : OCRDocumentAnalyser {
    override fun extractTextFrom(file: File): ExtractedText {
        val extractedText = tesseract.doOCR(file)

        return ExtractedText(extractedText)
    }
}