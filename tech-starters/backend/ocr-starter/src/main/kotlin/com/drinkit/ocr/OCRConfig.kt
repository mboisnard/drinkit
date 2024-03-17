package com.drinkit.ocr

import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class OCRConfig {

    @Bean
    fun createTesseractBean(): ITesseract {
        return Tesseract()
    }
}
