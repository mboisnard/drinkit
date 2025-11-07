package com.drinkit.documentation.processor.ksp

import com.drinkit.documentation.clean.architecture.CoreDomain
import com.drinkit.documentation.clean.architecture.Usecase
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

/**
 * KSP processor that orchestrates documentation generation from [CoreDomain] and [Usecase] annotations.
 */
internal class DocumentationSymbolProcessor(
    private val createCoreDomainDocumentation: CreateCoreDomainDocumentation,
    private val createCoreDomainsSummaryDocumentation: CreateCoreDomainsSummaryDocumentation,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private lateinit var coreDomainsByPackage: Map<String, List<CoreDomainInfo>>
    private lateinit var useCasesByPackage: Map<String, List<UseCaseInfo>>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        coreDomainsByPackage = collectInfo<CoreDomain, CoreDomainInfo>(
            resolver, KSClassDeclaration::toCoreDomain
        )
        useCasesByPackage = collectInfo<Usecase, UseCaseInfo>(
            resolver, KSClassDeclaration::toUseCase
        )

        return emptyList()
    }

    override fun finish() {
        val allDomains = coreDomainsByPackage.values.flatten()

        if (allDomains.isEmpty()) {
            logger.warn("No @CoreDomain found. Skipping documentation generation.")
            return
        }

        logger.warn("Generating documentation for ${allDomains.size} core domain(s)...")

        allDomains.forEach { domain ->
            val associatedUseCases = domain.findAssociatedUseCases(useCasesByPackage)
            createCoreDomainDocumentation.invoke(domain, associatedUseCases)
        }
        createCoreDomainsSummaryDocumentation.invoke(allDomains)

        logger.warn("Documentation generation completed")
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

    private inline fun <reified T : Annotation> KSClassDeclaration.findAnnotation(): KSAnnotation {
        val qualifiedName = T::class.qualifiedName!!
        return annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
        }
    }
}

private fun KSClassDeclaration.toCoreDomain(annotation: KSAnnotation): CoreDomainInfo {
    val description = annotation.getStringArgument(CoreDomain::description.name)
        ?: docString?.trim()

    return CoreDomainInfo(
        packageName = packageName.asString(),
        className = simpleName.asString(),
        customName = annotation.getStringArgument(CoreDomain::name.name),
        description = description,
    )
}

private fun KSClassDeclaration.toUseCase(annotation: KSAnnotation): UseCaseInfo {
    val description = annotation.getStringArgument(Usecase::description.name)
        ?: docString?.trim()

    return UseCaseInfo(
        packageName = packageName.asString(),
        className = simpleName.asString(),
        customName = annotation.getStringArgument(Usecase::name.name),
        description = description,
    )
}

private fun KSAnnotation.getStringArgument(argName: String): String? =
    (arguments.find { it.name?.asString() == argName }?.value as? String)
            ?.takeIf { it.isNotBlank() }