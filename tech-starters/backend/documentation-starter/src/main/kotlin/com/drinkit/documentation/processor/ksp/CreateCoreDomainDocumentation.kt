package com.drinkit.documentation.processor.ksp

import com.google.devtools.ksp.processing.KSPLogger
import java.io.File

internal class CreateCoreDomainDocumentation(
    private val outputFolderPath: String,
    private val logger: KSPLogger,
) {
    private val outputDirectory: File by lazy { File(outputFolderPath).apply { mkdirs() } }

    fun invoke(domain: CoreDomainInfo, useCases: List<UseCaseInfo>) {
        logger.warn("  Processing: ${domain.displayName}")

        val outputFile = File(outputDirectory, "${domain.fileName}.md")
        outputFile.writeText(generateMarkdown(domain, useCases))

        logger.warn("    Generated: ${outputFile.name} (${useCases.size} use case(s))")
    }

    private fun generateMarkdown(domain: CoreDomainInfo, useCases: List<UseCaseInfo>): String = buildString {
        appendLine("# ${domain.displayName}")
        appendLine()

        domain.description?.let {
            appendLine(it)
            appendLine()
        }

        if (useCases.isNotEmpty()) {
            appendLine("*Detected Use Cases:*")
            appendLine()

            useCases.sortedBy { it.displayName }.forEach { useCase ->
                appendLine("### ${useCase.displayName}")
                appendLine()

                useCase.description?.let {
                    appendLine(it)
                    appendLine()
                }

                appendLine("**Class:** `${useCase.qualifiedName}`")
                appendLine()
            }
        } else {
            appendLine("*No use cases found for this core domain.*")
            appendLine()
        }

        appendLine("---")
        appendLine("*Generated automatically from code annotations*")
    }
}
