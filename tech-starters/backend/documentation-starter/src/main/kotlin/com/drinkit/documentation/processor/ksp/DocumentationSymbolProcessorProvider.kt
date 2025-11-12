package com.drinkit.documentation.processor.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

private const val DOMAINS_FOLDER = "/domains"
private const val TECH_STARTERS_FOLDER = "/tech-starters"

internal class DocumentationSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val logger = environment.logger
        val docsOutputDir = environment.options["docsOutputDir"]
        val moduleName = environment.options["moduleName"] ?: "unknown-module-name"
        val domainsOutputPath = "$docsOutputDir$DOMAINS_FOLDER"
        val techStartersOutputPath = "$docsOutputDir$TECH_STARTERS_FOLDER"

        return DocumentationSymbolProcessor(
            createCoreDomainDocumentation = CreateCoreDomainDocumentation(domainsOutputPath, logger),
            createCoreDomainsOverviewDocumentation = CreateCoreDomainsOverviewDocumentation(domainsOutputPath, logger),
            createTechStarterToolDocumentation = CreateTechStarterToolDocumentation(
                outputFolderPath = techStartersOutputPath,
                logger = logger,
            ),
            createTechStarterToolsOverviewDocumentation = CreateTechStarterToolsOverviewDocumentation(
                outputFolderPath = techStartersOutputPath,
                logger = logger,
            ),
            logger = logger,
            moduleName = moduleName,
        )
    }
}
