package com.drinkit.documentation.processor.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

private const val DOMAINS_FOLDER = "/domains"

internal class DocumentationSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val logger = environment.logger
        val outputFolderPath = "${environment.options["docsOutputDir"]}$DOMAINS_FOLDER"

        return DocumentationSymbolProcessor(
            createCoreDomainDocumentation = CreateCoreDomainDocumentation(outputFolderPath, logger),
            createCoreDomainsSummaryDocumentation = CreateCoreDomainsSummaryDocumentation(outputFolderPath, logger),
            logger = logger,
        )
    }
}
