package com.drinkit.documentation.processor.ksp

import com.google.devtools.ksp.processing.KSPLogger
import java.io.File

internal class CreateCoreDomainsSummaryDocumentation(
    private val outputFolderPath: String,
    private val logger: KSPLogger,
) {

    fun invoke(domains: List<CoreDomainInfo>) {
        logger.warn("  Processing Core Domains Summary")

        val outputFile = File(outputFolderPath, "domains.md")
        outputFile.writeText(generateMarkdown(domains))

        logger.warn("    Generated: ${outputFile.name}")
    }

    private fun generateMarkdown(domains: List<CoreDomainInfo>): String = buildString {
        appendLine("# Domains Overview")
        appendLine()

        domains.sortedBy { it.displayName }
            .forEach { domain ->
                appendLine("- [${domain.displayName}](${domain.fileName}.md)")
                appendLine()
            }

        appendLine("---")
        appendLine("*Generated automatically from code annotations*")
    }
}
