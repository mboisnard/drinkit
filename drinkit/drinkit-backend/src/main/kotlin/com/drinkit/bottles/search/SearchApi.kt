package com.drinkit.bottles.search

import com.drinkit.api.generated.api.SearchApiDelegate
import com.drinkit.config.AbstractApi
import com.drinkit.ocr.OCRAnalysis
import com.drinkit.ocr.OCRResponse
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.HeadersBuilder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
@PreAuthorize("isAuthenticated()")
internal class SearchApi(
    private val ocrAnalysis: OCRAnalysis,
) : SearchApiDelegate, AbstractApi() {

    override fun searchByPhotoUpload(file: Resource?): ResponseEntity<Unit> {
        val ocrResponse = ocrAnalysis.extractText(file!!, locale())

        return when (ocrResponse) {
            is OCRResponse.ExtractedText -> ResponseEntity.ok().build()
            is OCRResponse.Error -> ResponseEntity.badRequest().build()
            is OCRResponse.FeatureDisabled -> forbidden().build()
        }
    }
}

fun forbidden(): HeadersBuilder<*> {
    return ResponseEntity.status(FORBIDDEN)
}
