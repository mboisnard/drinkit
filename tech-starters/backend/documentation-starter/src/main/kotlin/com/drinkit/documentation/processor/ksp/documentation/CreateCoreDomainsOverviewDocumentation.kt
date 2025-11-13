package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.CoreDomainInfo
import com.drinkit.documentation.processor.ksp.UseCaseInfo
import com.google.devtools.ksp.processing.KSPLogger
import java.io.File

internal class CreateCoreDomainsOverviewDocumentation(
    private val outputFolderPath: String,
    private val logger: KSPLogger,
) {
    private val outputFile: File by lazy {
        File(outputFolderPath, "domains.md").apply { parentFile?.mkdirs() }
    }

    fun invoke(domains: List<CoreDomainInfo>, useCasesByPackage: Map<String, List<UseCaseInfo>>) {
        logger.warn("  Updating core domains overview...")

        val newEntries = domains.map { domain ->
            val useCasesCount = domain.findAssociatedUseCases(useCasesByPackage).size
            DomainEntry(domain.displayName, domain.fileName, useCasesCount)
        }

        val updatedEntries = fetchExistingEntries()
            .filterNot { existingEntry -> newEntries.any { it.fileName == existingEntry.fileName } }
            .plus(newEntries)
            .sortedBy { it.displayName }

        outputFile.writeText(
            generateMarkdown(updatedEntries)
        )

        logger.warn("    Updated overview with ${domains.size} domain(s)")
    }

    private fun fetchExistingEntries(): List<DomainEntry> {
        if (!outputFile.exists())
            return emptyList()

        return outputFile.readLines()
            .filter { it.startsWith("- [") }
            .mapNotNull { line ->
                // Format: - [Domain Name](./domain-file.md) (X use cases)
                val displayName = line.substringAfter('[').substringBefore(']')
                val fileName = line.substringAfter("(./").substringBefore(".md)")
                val count = line.substringAfterLast('(').substringBefore(' ').toIntOrNull()
                if (displayName.isNotBlank() && fileName.isNotBlank() && count != null)
                    DomainEntry(displayName, fileName, count)
                else null
            }
    }

    private fun generateMarkdown(entries: List<DomainEntry>) = buildString {
        appendLine("# Domains")
        appendLine()
        appendLine("This section documents all core business domains and their associated use cases.")
        appendLine()
        appendLine("## Available Domains")
        appendLine()

        if (entries.isEmpty()) {
            appendLine("*No domains documented yet.*")
        } else {
            entries.forEach { entry ->
                val useCaseWord = if (entry.useCasesCount > 1) "use cases" else "use case"
                appendLine("- [${entry.displayName}](./${entry.fileName}.md) (${entry.useCasesCount} $useCaseWord)")
            }
        }

        appendLine()
        appendLine("---")
        appendLine("*Generated automatically from code annotations*")
    }

    private data class DomainEntry(
        val displayName: String,
        val fileName: String,
        val useCasesCount: Int,
    )
}
