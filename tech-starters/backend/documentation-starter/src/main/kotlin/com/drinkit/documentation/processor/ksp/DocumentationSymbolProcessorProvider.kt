package com.drinkit.documentation.processor.ksp

import com.drinkit.documentation.processor.ksp.documentation.CreateCoreDomainDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateCoreDomainsOverviewDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateTechStarterDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateTechStartersOverviewDocumentation
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

private const val DOMAINS_FOLDER = "/domains"
private const val TECH_STARTERS_FOLDER = "/tech-starters"

internal class DocumentationSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val logger = environment.logger
        val docsOutputDir = environment.options["docsOutputDir"]
        val moduleName = environment.options["moduleName"]!!
        val moduleSourceDir = environment.options["moduleSourceDir"]!!
        val domainsOutputPath = "$docsOutputDir$DOMAINS_FOLDER"
        val techStartersOutputPath = "$docsOutputDir$TECH_STARTERS_FOLDER"

        return DocumentationSymbolProcessor(
            createCoreDomainDocumentation = CreateCoreDomainDocumentation(domainsOutputPath, logger),
            createCoreDomainsOverviewDocumentation = CreateCoreDomainsOverviewDocumentation(domainsOutputPath, logger),
            createTechStarterDocumentation = CreateTechStarterDocumentation(
                outputFolderPath = techStartersOutputPath,
                moduleSourceDir = moduleSourceDir,
                logger = logger
            ),
            createTechStartersOverviewDocumentation = CreateTechStartersOverviewDocumentation(techStartersOutputPath, logger),
            logger = logger,
            moduleName = moduleName,
        )
    }
}
