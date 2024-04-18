package com.drinkit.ocr

import java.io.File
import java.util.*

data class ExtractedText(
    val rawValue: String,
    val sanitizedValue: String,
)

fun interface OCRDocumentAnalyser {

    fun extractTextFrom(file: File, locale: Locale): ExtractedText
}
