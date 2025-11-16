package com.drinkit.documentation.processor.ksp.documentation

import com.google.devtools.ksp.processing.KSPLogger
import java.io.File

internal class CreateTechStartersOverviewDocumentation(
    private val outputFolderPath: String,
    private val logger: KSPLogger,
) {
    private val outputFile: File by lazy {
        File(outputFolderPath, "tech-starters.md").apply { parentFile?.mkdirs() }
    }

    fun invoke(moduleName: String, toolsCount: Int) {
        logger.warn("  Updating tech starters overview...")

        val updatedEntries = fetchExistingEntries()
            .filterNot { it.moduleName == moduleName }
            .plus(ModuleEntry(moduleName, toolsCount))
            .sortedBy { it.moduleName }

        outputFile.writeText(
            generateMarkdown(updatedEntries)
        )

        logger.warn("    Updated overview with module: $moduleName")
    }

    private fun fetchExistingEntries(): List<ModuleEntry> {
        if (!outputFile.exists())
            return emptyList()

        return outputFile.readLines()
            .filter { it.startsWith("- [") }
            .mapNotNull { line ->
                // Format: - [module-name](./module-name.md) (X tools)
                val name = line.substringAfter('[').substringBefore(']')
                val count = line.substringAfterLast('(').substringBefore(' ').toIntOrNull()
                if (name.isNotBlank() && count != null) ModuleEntry(name, count) else null
            }
    }

    private fun generateMarkdown(entries: List<ModuleEntry>) = buildString {
        appendLine("# Tech Starters")
        appendLine()
        appendLine("This section documents all available tech starters and their associated tools.")
        appendLine()
        appendLine("## Available Modules")
        appendLine()

        if (entries.isEmpty()) {
            appendLine("*No modules documented yet.*")
        } else {
            entries.forEach { entry ->
                val toolWord = if (entry.toolsCount > 1) "tools" else "tool"
                appendLine("- [${entry.moduleName}](./${entry.moduleName}.md) (${entry.toolsCount} $toolWord)")
            }
        }

        appendLine()
        appendLine("---")
        appendLine("*Generated automatically from code analysis*")
    }

    private data class ModuleEntry(
        val moduleName: String,
        val toolsCount: Int,
    )
}
