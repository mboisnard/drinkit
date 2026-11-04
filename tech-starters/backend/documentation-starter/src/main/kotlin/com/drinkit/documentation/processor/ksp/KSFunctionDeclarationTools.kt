package com.drinkit.documentation.processor.ksp

import com.drinkit.documentation.tech.starter.TechStarterTool
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference

internal fun KSFunctionDeclaration.toTechStarterFunction(annotation: KSAnnotation): TechStarterFunctionInfo {
    val description = annotation.getStringArgument(TechStarterTool::description.name)
        ?: docString

    return TechStarterFunctionInfo(
        packageName = packageName.asString(),
        name = simpleName.asString(),
        customName = annotation.getStringArgument(TechStarterTool::name.name),
        description = description?.sanitizeMarkdown(),
        signature = signature(),
    )
}

private fun KSFunctionDeclaration.signature(): String {
    val typeParameters = typeParameters.takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "<", postfix = "> ") { it.render() }
        .orEmpty()
    val receiver = extensionReceiver?.let { "${it.render()}." }.orEmpty()
    val parameters = parameters.joinToString { parameter ->
        val default = if (parameter.hasDefault) " = ..." else ""
        "${parameter.name?.asString()}: ${parameter.type.render()}$default"
    }
    val returns = returnType?.render()?.takeUnless { it == "Unit" }?.let { ": $it" }.orEmpty()
    return "fun $typeParameters$receiver${simpleName.asString()}($parameters)$returns"
}

private fun KSTypeParameter.render(): String {
    val bound = bounds.map { it.render() }.firstOrNull { it != "Any?" }
    return if (bound == null) name.asString() else "${name.asString()} : $bound"
}

private fun KSTypeReference.render(): String = resolve().render()

private fun KSType.render(): String {
    val typeArguments = arguments.takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "<", postfix = ">") { it.type?.render() ?: "*" }
        .orEmpty()
    val nullable = if (isMarkedNullable) "?" else ""
    return "${declaration.simpleName.asString()}$typeArguments$nullable"
}
