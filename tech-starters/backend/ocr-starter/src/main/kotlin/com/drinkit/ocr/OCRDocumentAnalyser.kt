package com.drinkit.ocr

import java.io.File

data class ExtractedText(
    val value: String,
)

fun interface OCRDocumentAnalyser {

    fun extractTextFrom(file: File): ExtractedText
}