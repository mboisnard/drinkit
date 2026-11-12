package com.drinkit.documentation.processor.ksp

import com.drinkit.documentation.clean.architecture.CoreDomain
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.processor.ksp.documentation.CoreDomainInfo
import com.drinkit.documentation.processor.ksp.documentation.CreateCoreDomainDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateCoreDomainsOverviewDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateTechStarterDocumentation
import com.drinkit.documentation.processor.ksp.documentation.CreateTechStartersOverviewDocumentation
import com.drinkit.documentation.processor.ksp.documentation.TechStarterToolInfo
import com.drinkit.documentation.processor.ksp.documentation.TechStarterTopLevelInfo
import com.drinkit.documentation.processor.ksp.documentation.UseCaseInfo
import com.drinkit.documentation.tech.starter.TechStarterTool
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
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

    private lateinit var coreDomains: List<CoreDomainInfo>
    private lateinit var useCasesByPackage: Map<String, List<UseCaseInfo>>
    private lateinit var techStarterTools: List<TechStarterToolInfo>
    private lateinit var techStarterTopLevelTools: List<TechStarterTopLevelInfo>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        coreDomains = collectInfo<CoreDomain, KSClassDeclaration, _>(resolver, KSClassDeclaration::toCoreDomain)
        useCasesByPackage = collectInfo<Usecase, KSClassDeclaration, _>(resolver, KSClassDeclaration::toUseCase)
            .groupBy { it.packageName }
        techStarterTools = collectInfo<TechStarterTool, KSClassDeclaration, _>(
            resolver,
            KSClassDeclaration::toTechStarterTool,
        )
        techStarterTopLevelTools = collectInfo<TechStarterTool, KSFunctionDeclaration, _>(
            resolver,
            KSFunctionDeclaration::toTechStarterFunction,
        ) + collectInfo<TechStarterTool, KSPropertyDeclaration, _>(
            resolver,
            KSPropertyDeclaration::toTechStarterProperty,
        )

        return emptyList()
    }

    override fun finish() {
        if (coreDomains.isNotEmpty()) {
            logger.warn("Generating documentation for ${coreDomains.size} core domain(s)...")

            coreDomains.forEach { domain ->
                val associatedUseCases = domain.findAssociatedUseCases(useCasesByPackage)
                createCoreDomainDocumentation.invoke(domain, associatedUseCases)
            }
            createCoreDomainsOverviewDocumentation.invoke(coreDomains, useCasesByPackage)

            logger.warn("Core domain documentation generation completed")
        }

        if (techStarterTools.isNotEmpty() || techStarterTopLevelTools.isNotEmpty() || moduleName.endsWith("-starter")) {
            logger.warn("Generating documentation for tech starter tools")

            createTechStarterDocumentation.invoke(moduleName, techStarterTools, techStarterTopLevelTools)
            createTechStartersOverviewDocumentation.invoke(
                moduleName,
                techStarterTools.size + techStarterTopLevelTools.size,
            )

            logger.warn("Tech starter tools documentation generation completed")
        }
    }

    private inline fun <reified A : Annotation, reified D : KSAnnotated, T> collectInfo(
        resolver: Resolver,
        noinline mapper: (D, KSAnnotation) -> T,
    ): List<T> = resolver.getSymbolsWithAnnotation(A::class.qualifiedName!!)
        .filterIsInstance<D>()
        .filter { it.validate(enableNewFeatures = false) }
        // A member is documented with its class: only a top-level function or property is a tool of its own
        .filter { it !is KSDeclaration || it is KSClassDeclaration || it.parentDeclaration == null }
        .map { declaration -> mapper(declaration, declaration.findAnnotation<A>()) }
        .toList()
}
