package com.drinkit.bottle.search

import com.drinkit.api.generated.api.SearchApi
import com.drinkit.config.AbstractApi
import com.drinkit.ocr.OCRAnalysis
import com.drinkit.ocr.OCRResponse
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.HeadersBuilder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@PreAuthorize("isAuthenticated()")
internal class SearchController(private val ocrAnalysis: OCRAnalysis) :
    AbstractApi(),
    SearchApi {

    override fun searchByPhotoUpload(file: MultipartFile): ResponseEntity<Unit> {
        val ocrResponse = ocrAnalysis.extractText(file.resource, locale())

        return when (ocrResponse) {
            is OCRResponse.ExtractedText -> ResponseEntity.ok().build()
            is OCRResponse.Error -> ResponseEntity.badRequest().build()
            is OCRResponse.FeatureDisabled -> forbidden().build()
        }
    }
}

fun forbidden(): HeadersBuilder<*> = ResponseEntity.status(FORBIDDEN)
