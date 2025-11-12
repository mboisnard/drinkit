package com.drinkit.documentation.processor.ksp

import com.drinkit.documentation.clean.architecture.CoreDomain
import com.drinkit.documentation.clean.architecture.Usecase
import com.drinkit.documentation.tech.starter.TechStarterTool
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration

internal inline fun <reified T : Annotation> KSClassDeclaration.findAnnotation(): KSAnnotation {
    val qualifiedName = T::class.qualifiedName!!
    return annotations.first {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
    }
}

internal fun KSClassDeclaration.toCoreDomain(annotation: KSAnnotation): CoreDomainInfo {
    val description = annotation.getStringArgument(CoreDomain::description.name)
        ?: docString

    return CoreDomainInfo(
        packageName = packageName.asString(),
        className = simpleName.asString(),
        customName = annotation.getStringArgument(CoreDomain::name.name),
        description = description?.sanitizeMarkdown(),
    )
}

internal fun KSClassDeclaration.toUseCase(annotation: KSAnnotation): UseCaseInfo {
    val description = annotation.getStringArgument(Usecase::description.name)
        ?: docString

    return UseCaseInfo(
        packageName = packageName.asString(),
        className = simpleName.asString(),
        customName = annotation.getStringArgument(Usecase::name.name),
        description = description?.sanitizeMarkdown(),
    )
}

internal fun KSClassDeclaration.toTechStarterTool(annotation: KSAnnotation): TechStarterToolInfo {
    val description = annotation.getStringArgument(TechStarterTool::description.name)
        ?: docString

    return TechStarterToolInfo(
        packageName = packageName.asString(),
        className = simpleName.asString(),
        customName = annotation.getStringArgument(TechStarterTool::name.name),
        description = description?.sanitizeMarkdown(),
        methods = extractMethods(),
    )
}

private fun KSAnnotation.getStringArgument(argName: String): String? =
    (arguments.find { it.name?.asString() == argName }?.value as? String)
            ?.takeIf { it.isNotBlank() }

private fun KSClassDeclaration.extractMethods(): List<MethodInfo> =
    getAllFunctions()
        .filter { !it.simpleName.asString().startsWith("component") }
        .filter { !it.simpleName.asString().startsWith("copy") }
        .filter { !listOf("<init>", "equals", "hashCode", "toString").contains(it.simpleName.asString()) }
        .map { function ->
            MethodInfo(
                name = function.simpleName.asString(),
                description = function.docString?.sanitizeMarkdown(),
            )
        }
        .toList()

/**
 * Escapes special Markdown characters to prevent them from being interpreted as markup.
 */
private fun String.sanitizeMarkdown(): String =
    this.trim().replace("\\", "\\\\")
        .replace("<", "\\<")
        .replace(">", "\\>")