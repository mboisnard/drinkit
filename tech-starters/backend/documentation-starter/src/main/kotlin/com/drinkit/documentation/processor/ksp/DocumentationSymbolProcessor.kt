package com.drinkit.documentation.processor.ksp

import com.drinkit.documentation.clean.architecture.CoreDomain
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.processor.ksp.documentation.CreateCoreDomainDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateCoreDomainsOverviewDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateTechStarterDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateTechStartersOverviewDocumentation
import com.drinkit.documentation.tech.starter.TechStarterTool
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

/**
 * KSP processor that orchestrates documentation generation from [CoreDomain], [Usecase] and [TechStarterTool] annotations.
 */
internal class DocumentationSymbolProcessor(
    private val createCoreDomainDocumentation: CreateCoreDomainDocumentation,
    private val createCoreDomainsOverviewDocumentation: CreateCoreDomainsOverviewDocumentation,
    private val createTechStarterDocumentation: CreateTechStarterDocumentation,
    private val createTechStartersOverviewDocumentation: CreateTechStartersOverviewDocumentation,
    private val moduleName: String,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private lateinit var coreDomainsByPackage: Map<String, List<CoreDomainInfo>>
    private lateinit var useCasesByPackage: Map<String, List<UseCaseInfo>>
    private lateinit var techStarterToolsByPackage: Map<String, List<TechStarterToolInfo>>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        coreDomainsByPackage = collectInfo<CoreDomain, CoreDomainInfo>(
            resolver, KSClassDeclaration::toCoreDomain
        )
        useCasesByPackage = collectInfo<Usecase, UseCaseInfo>(
            resolver, KSClassDeclaration::toUseCase
        )
        techStarterToolsByPackage = collectInfo<TechStarterTool, TechStarterToolInfo>(
            resolver, KSClassDeclaration::toTechStarterTool
        )

        return emptyList()
    }

    override fun finish() {
        val allDomains = coreDomainsByPackage.values.flatten()

        if (allDomains.isNotEmpty()) {
            logger.warn("Generating documentation for ${allDomains.size} core domain(s)...")

            allDomains.forEach { domain ->
                val associatedUseCases = domain.findAssociatedUseCases(useCasesByPackage)
                createCoreDomainDocumentation.invoke(domain, associatedUseCases)
            }
            createCoreDomainsOverviewDocumentation.invoke(allDomains, useCasesByPackage)

            logger.warn("Core domain documentation generation completed")
        }

        val allTools = techStarterToolsByPackage.values.flatten()

        if (allTools.isNotEmpty() || moduleName.endsWith("-starter")) {
            logger.warn("Generating documentation for tech starter tools")

            createTechStarterDocumentation.invoke(moduleName, allTools)
            createTechStartersOverviewDocumentation.invoke(moduleName, allTools.size)

            logger.warn("Tech starter tools documentation generation completed")
        }
    }

    private inline fun <reified A : Annotation, T : AnnotatedInfo> collectInfo(
        resolver: Resolver,
        noinline mapper: (KSClassDeclaration, KSAnnotation) -> T
    ): Map<String, List<T>> =
        resolver.getSymbolsWithAnnotation(A::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .map { classDeclaration ->
                val annotation = classDeclaration.findAnnotation<A>()
                mapper(classDeclaration, annotation)
            }
            .groupBy { it.packageName }
}