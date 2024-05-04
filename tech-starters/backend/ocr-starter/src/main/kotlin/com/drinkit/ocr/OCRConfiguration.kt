package com.drinkit.ocr

import com.drinkit.ocr.analyzers.AnalyzerStatus
import com.drinkit.ocr.analyzers.GCPCloudVisionAnalyzer
import com.drinkit.ocr.analyzers.OCRAnalyzers
import com.drinkit.ocr.analyzers.OCRAvailableAnalyzer
import com.drinkit.ocr.analyzers.TesseractAnalyzer
import com.google.cloud.spring.autoconfigure.vision.CloudVisionProperties
import com.google.cloud.spring.core.DefaultCredentialsProvider
import com.google.cloud.spring.core.UserAgentHeaderProvider
import com.google.cloud.spring.vision.CloudVisionTemplate
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.cloud.vision.v1.ImageAnnotatorSettings
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
@EnableConfigurationProperties(CloudVisionProperties::class)
internal class OCRConfiguration(
    private val environment: Environment,
    private val cloudVisionProperties: CloudVisionProperties,
) {

    @Bean
    fun createOCRAnalyzers(): OCRAnalyzers {
        val analyzersWithStatus = OCRAvailableAnalyzer.entries
            .associateWith { it.computeStatus(environment) }

        val instances = analyzersWithStatus
            .filter { (_, status) -> status == AnalyzerStatus.RUNNING }
            .keys
            .associateWith {
                when (it) {
                    OCRAvailableAnalyzer.GOOGLE_CLOUD_VISION -> createGoogleCloudVisionAnalyzer()
                    OCRAvailableAnalyzer.TESSERACT -> TesseractAnalyzer()
                }
            }

        return OCRAnalyzers(analyzersWithStatus, instances)
    }

    private fun createGoogleCloudVisionAnalyzer(): GCPCloudVisionAnalyzer {
        val googleCredentials = DefaultCredentialsProvider(cloudVisionProperties)
        val imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
            .setCredentialsProvider(googleCredentials)
            .setHeaderProvider(UserAgentHeaderProvider(OCRConfiguration::class.java))
            .build()
        val imageAnnotatorClient = ImageAnnotatorClient.create(imageAnnotatorSettings)
        val cloudVisionTemplate = CloudVisionTemplate(imageAnnotatorClient)

        return GCPCloudVisionAnalyzer(cloudVisionTemplate)
    }
}
