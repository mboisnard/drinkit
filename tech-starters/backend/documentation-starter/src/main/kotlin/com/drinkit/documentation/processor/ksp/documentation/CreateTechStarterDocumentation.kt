package com.drinkit.documentation.processor.ksp.documentation

import com.drinkit.documentation.processor.ksp.TechStarterFunctionInfo
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

    fun invoke(moduleName: String, tools: List<TechStarterToolInfo>, functions: List<TechStarterFunctionInfo>) {
        logger.warn("  Processing module: $moduleName")

        val readmeContent = readReadmeIfExists()
        val outputFile = File(outputDirectory, "$moduleName.md")
        outputFile.writeText(
            generateMarkdown(moduleName, tools, functions, readmeContent),
        )

        logger.warn("    Generated: ${outputFile.name} (${tools.size + functions.size} tool(s))")
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
        functions: List<TechStarterFunctionInfo>,
        readmeContent: String?,
    ) = buildString {
        if (readmeContent != null) {
            appendLine(readmeContent)
            appendLine()

            if (tools.isNotEmpty() || functions.isNotEmpty()) {
                appendLine("---")
                appendLine()
                appendLine("## API Reference")
                appendLine()
            }
        } else {
            appendLine("# $moduleName")
            appendLine()
        }

        tools.sortedBy { it.displayName }.forEach { tool ->
            appendLine("### ${tool.displayName}")
            appendLine()
            appendDescription(tool.description)
            appendLine("**Class**: `${tool.qualifiedName}`")
            appendLine()

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

        functions.sortedBy { it.displayName }.forEach { function ->
            appendLine("### ${function.displayName}")
            appendLine()
            appendDescription(function.description)
            appendLine("**Function**: `${function.packageName}.${function.name}`")
            appendLine()
            appendLine("```kotlin")
            appendLine(function.signature)
            appendLine("```")
            appendLine()
        }

        appendLine("---")
        appendLine("*Generated automatically from code annotations*")
    }

    private fun StringBuilder.appendDescription(description: String?) {
        description?.let {
            it.lines().forEach { line -> appendLine("> $line") }
            appendLine()
        }
    }
}
