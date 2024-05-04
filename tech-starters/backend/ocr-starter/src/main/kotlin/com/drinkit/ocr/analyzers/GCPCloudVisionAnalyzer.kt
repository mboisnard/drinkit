package com.drinkit.ocr.analyzers

import com.drinkit.ocr.OCRResponse
import com.google.api.gax.rpc.ApiException
import com.google.cloud.spring.vision.CloudVisionException
import com.google.cloud.spring.vision.CloudVisionTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.Resource
import java.util.*

internal class GCPCloudVisionAnalyzer(
    private val cloudVisionTemplate: CloudVisionTemplate,
) : OCRAnalyzer {

    private val logger = KotlinLogging.logger { }

    override fun extractTextFrom(resource: Resource, locale: Locale): OCRResponse {
        return try {
            val extractedText = cloudVisionTemplate.extractTextFromImage(resource)
            OCRResponse.ExtractedText(extractedText)
        } catch (ex: CloudVisionException) {
            logger.error(ex) { "Cannot use Google cloud vision API to extract text from image" }
            OCRResponse.Error(ex.message!!)
        } catch (ex: ApiException) { // TODO Improve multi catch management
            logger.error(ex) { "Cannot use Google cloud vision API to extract text from image" }
            OCRResponse.Error(ex.message!!)
        }
    }
}
