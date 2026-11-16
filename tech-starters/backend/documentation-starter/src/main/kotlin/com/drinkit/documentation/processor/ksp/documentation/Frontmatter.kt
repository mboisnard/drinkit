package com.drinkit.documentation.processor.ksp.documentation

// The site shows the title of a page from its frontmatter, so a page carries no heading of level one
internal fun StringBuilder.appendFrontmatter(title: String, description: String? = null) {
    appendLine("---")
    appendLine("title: ${title.quoted()}")
    description?.let { appendLine("description: ${it.quoted()}") }
    appendLine("---")
    appendLine()
}

private fun String.quoted() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
