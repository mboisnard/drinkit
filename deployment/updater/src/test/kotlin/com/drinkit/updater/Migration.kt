package com.drinkit.updater

import liquibase.change.core.RawSQLChange
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.parser.core.formattedsql.FormattedSqlChangeLogParser
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.Resource
import liquibase.util.StringUtil

private const val CHANGELOG = "db/changelog"
private const val MASTER = "db.changelog-master.yml"
private val resourceAccessor = ClassLoaderResourceAccessor()
private val parser = FormattedSqlChangeLogParser()

internal class Migration(private val resource: Resource) {

    // Such as base/01-initial-schema.sql
    val path: String = resource.path.substringAfter("$CHANGELOG/")

    val fileName: String = path.substringAfterLast('/')

    val folder: String = path.substringBeforeLast('/', missingDelimiterValue = "")

    val lines: List<String> = resource.openInputStream().bufferedReader().use { it.readLines() }

    val isFormattedSql: Boolean = parser.supports(resource.path, resourceAccessor)

    val changeSets: List<ChangeSet> by lazy {
        parser.parse(resource.path, ChangeLogParameters(), resourceAccessor).changeSets
    }

    val sqlChanges: List<RawSQLChange> by lazy { changeSets.flatMap { it.changes }.filterIsInstance<RawSQLChange>() }

    // Split as Liquibase sends them to PostgreSQL
    val statements: List<Statement> by lazy {
        sqlChanges.flatMap { sql ->
            StringUtil.processMultiLineSQL(sql.sql, true, sql.isSplitStatements != false, sql.endDelimiter, null)
                .map(::Statement)
        }
    }

    override fun toString() = path

    companion object {
        const val BASE = "base"
        const val DDL = "ddl"
        const val DML = "dml"

        fun all(): List<Migration> = resourceAccessor.search(CHANGELOG, true).asSequence()
            .filterNot { it.path.endsWith("/$MASTER") }
            .map(::Migration)
            .sortedBy { it.path }
            .toList()
    }
}

internal class Statement(text: String) {

    private val words = text.uppercase().split(WHITESPACE).filter { it.isNotEmpty() }

    val firstWord: String? = words.firstOrNull()

    val setting: String? = if (firstWord == "SET") words.getOrNull(1)?.substringBefore('=') else null

    val indexBuiltConcurrently: String? =
        if (firstWord == "CREATE" && "INDEX" in words && "CONCURRENTLY" in words) {
            words.getOrNull(words.indexOf("ON") - 1)
        } else {
            null
        }

    val indexDroppedConcurrently: String? =
        if (startsWith("DROP INDEX CONCURRENTLY IF EXISTS")) words.last().substringAfterLast('.') else null

    fun startsWith(beginning: String): Boolean = beginning.split(' ').let { words.take(it.size) == it }

    companion object {
        private val WHITESPACE = Regex("""\s+""")
        val NONE = Statement("")
    }
}
