package com.drinkit.documentation.processor.ksp

import kotlin.collections.filter
import kotlin.collections.isNotEmpty

private val CAMEL_CASE_DETECTION_REGEX = Regex("(?<=[a-z])([A-Z])")

internal sealed interface AnnotatedInfo {
    val packageName: String
    val className: String

    val customName: String?
    val description: String?

    val displayName: String
        get() = customName ?: className.replace(CAMEL_CASE_DETECTION_REGEX, " $1")

    val qualifiedName: String
        get() = "$packageName.$className"
}

internal data class CoreDomainInfo(
    override val packageName: String,
    override val className: String,
    override val customName: String?,
    override val description: String?,
) : AnnotatedInfo {
    val fileName = displayName.lowercase().replace(" ", "-")

    fun findAssociatedUseCases(useCasesByPackage: Map<String, List<UseCaseInfo>>): List<UseCaseInfo> {
        // Strategy 1: Usecases located on the same package
        useCasesByPackage[packageName]
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        val parentPackage = packageName.substringBeforeLast('.')

        // Strategy 2: Usecases located in the parent package and packageName contains the domain name
        if (parentPackage.contains(className, ignoreCase = true)) {
            useCasesByPackage[parentPackage]
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        // Strategy 3: Usecases located in the parent package and contains the domain name in their class name
        useCasesByPackage[parentPackage]
            ?.filter { it.matchesDomain(this) }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        // Strategy 4: Global search by name
        return useCasesByPackage.values
            .flatten()
            .filter { it.matchesDomain(this) }
    }
}

internal data class UseCaseInfo(
    override val packageName: String,
    override val className: String,
    override val customName: String?,
    override val description: String?,
) : AnnotatedInfo {

    fun matchesDomain(domain: CoreDomainInfo): Boolean =
        domain.customName?.let { className.contains(it.replace(" ", ""), ignoreCase = true) } ?: false ||
        className.contains(domain.className, ignoreCase = true)
}