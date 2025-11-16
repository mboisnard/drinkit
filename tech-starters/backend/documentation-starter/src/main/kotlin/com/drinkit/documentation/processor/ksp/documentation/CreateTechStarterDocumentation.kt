package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.TechStarterToolInfo
import com.google.devtools.ksp.processing.KSPLogger
import java.io.File

internal class CreateTechStarterDocumentation(
    private val outputFolderPath: String,
    private val moduleSourceDir: String,
    private val logger: KSPLogger,
) {
    private val outputDirectory: File by lazy {
        File(outputFolderPath).apply { mkdirs() }
    }

    fun invoke(moduleName: String, tools: List<TechStarterToolInfo>) {
        logger.warn("  Processing module: $moduleName")

        val readmeContent = readReadmeIfExists()
        val outputFile = File(outputDirectory, "$moduleName.md")
        outputFile.writeText(
            generateMarkdown(moduleName, tools, readmeContent)
        )

        logger.warn("    Generated: ${outputFile.name} (${tools.size} tool(s))")
    }

    private fun readReadmeIfExists(): String? {
        val readmeFile = File(moduleSourceDir, "README.md")
        return if (readmeFile.exists()) {
            logger.warn("    Found README.md, including content in documentation")
            readmeFile.readText()
        } else {
            null
        }
    }

    private fun generateMarkdown(
        moduleName: String,
        tools: List<TechStarterToolInfo>,
        readmeContent: String?
    ) = buildString {
        if (readmeContent != null) {
            appendLine(readmeContent)
            appendLine()

            if (tools.isNotEmpty()) {
                appendLine("---")
                appendLine()
                appendLine("## API Reference")
                appendLine()
            }
        } else {
            appendLine("# $moduleName")
            appendLine()
        }

        // Tools details
        if (tools.isNotEmpty()) {
            tools.sortedBy { it.displayName }.forEach { tool ->
                appendLine("### ${tool.displayName}")
                appendLine()

                // Description
                tool.description?.let { description ->
                    description.lines().forEach { line ->
                        appendLine("> $line")
                    }
                    appendLine()
                }

                // Class info
                appendLine("**Class**: `${tool.qualifiedName}`")
                appendLine()

                // Methods in a collapsible section if there are any
                if (tool.methods.isNotEmpty()) {
                    appendLine("<details>")
                    appendLine("<summary><strong>Methods (${tool.methods.size})</strong></summary>")
                    appendLine()

                    tool.methods.sortedBy { it.name }.forEach { method ->
                        append("- **`${method.name}()`**")
                        method.description?.let { description ->
                            val inlineDescription = description.lines().joinToString(" ") { it.trim() }
                            append(" - $inlineDescription")
                        }
                        appendLine()
                    }

                    appendLine()
                    appendLine("</details>")
                    appendLine()
                }
            }
        }

        appendLine("---")
        appendLine("*Generated automatically from code annotations*")
    }
}
