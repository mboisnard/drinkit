package com.drinkit.updater

import com.drinkit.updater.Migration.Companion.BASE
import com.drinkit.updater.Migration.Companion.DDL
import com.drinkit.updater.Migration.Companion.DML
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ChangelogConventionsTest {

    private val migrations = Migration.all()
    private val liquibaseMigrations = migrations.filter { it.isFormattedSql }
    private val baseMigrations = liquibaseMigrations.filter { it.folder == BASE }
    private val ddlMigrations = liquibaseMigrations.filter { it.folder == DDL }
    private val dmlMigrations = liquibaseMigrations.filter { it.folder == DML }

    @Test
    fun `the migrations are found`() {
        migrations.shouldNotBeEmpty()
    }

    @Test
    fun `every migration starts with the formatted SQL header`() {
        // When
        val withoutHeader = migrations.filterNot { it.isFormattedSql }

        // Then
        withoutHeader shouldBreak rule(
            "formatted-sql-header",
            why = "without it, Liquibase runs the file as plain SQL and ignores every option of its changeset",
            fix = "start the file with the line `--liquibase formatted sql`",
        )
    }

    @Test
    fun `every migration holds one changeset named after its file`() {
        // When
        val misnamed = liquibaseMigrations.filterNot { migration ->
            migration.changeSets.singleOrNull()?.id == migration.fileName.removeSuffix(".sql")
        }

        // Then
        misnamed shouldBreak rule(
            "one-changeset-per-file",
            why = "the file and the run Liquibase records must name each other",
            fix = "write a single `--changeset <author>:<file name without .sql>`",
        )
    }

    @Test
    fun `every migration keeps a fixed logical file path`() {
        // When
        val movable = liquibaseMigrations.filterNot { migration -> migration.changeSets.all { it.filePath == "fixed" } }

        // Then
        movable shouldBreak rule(
            "fixed-logical-file-path",
            why = "Liquibase would run a moved file again",
            fix = "add `logicalFilePath:fixed` to the changeset",
        )
    }

    @Test
    fun `every migration runs its statements outside a transaction`() {
        // When
        val transactional = liquibaseMigrations.filter { migration ->
            migration.changeSets.any { it.isRunInTransaction }
        }

        // Then
        transactional shouldBreak rule(
            "no-transaction",
            why = "each statement then holds its locks alone, and `CONCURRENTLY` can run",
            fix = "add `runInTransaction:false` to the changeset",
        )
    }

    @Test
    fun `every migration lets Liquibase split its statements`() {
        // When
        val unsplit = liquibaseMigrations.filter { migration ->
            migration.sqlChanges.any { it.isSplitStatements == false }
        }

        // Then
        unsplit shouldBreak rule(
            "split-statements",
            why = "Liquibase and Squawk must see the same statements",
            fix = "remove `splitStatements:false`",
        )
    }

    @Test
    fun `no migration hides statements in a DO block`() {
        // When
        val withDoBlock = liquibaseMigrations.filter { migration -> migration.statements.any { it.firstWord == "DO" } }

        // Then
        withDoBlock shouldBreak rule(
            "no-do-block",
            why = "Squawk does not read the statements inside a `DO` block",
            fix = "write each statement on its own",
        )
    }

    @Test
    fun `every migration bounds its waits before its first change`() {
        // When
        val unbounded = liquibaseMigrations.filterNot { migration ->
            migration.statements.takeWhile { it.setting != null }.map { it.setting }.containsAll(TIMEOUTS)
        }

        // Then
        unbounded shouldBreak rule(
            "timeouts-first",
            why = "a statement that waits for its lock blocks every query queued behind it",
            fix = "start with `SET lock_timeout = '5s';` and `SET statement_timeout = '1min';`",
        )
    }

    @Test
    fun `every Squawk exception stands on its own line, below its reason`() {
        // When
        val unexplained = migrations.filter { migration ->
            (listOf("") + migration.lines).zipWithNext().any { (previous, line) ->
                SQUAWK_DIRECTIVE in line && !(line.isComment() && previous.isReason())
            }
        }

        // Then
        unexplained shouldBreak rule(
            "squawk-ignore-reason",
            why = "every tolerated violation must say why",
            fix = "put the directive on a line of its own, right below a `-- <reason>` comment",
        )
    }

    @Test
    fun `every migration sits directly in base, ddl or dml`() {
        // When
        val elsewhere = migrations.filterNot { it.folder in FOLDERS }

        // Then
        elsewhere shouldBreak rule(
            "known-folder",
            why = "Liquibase runs base/, then ddl/, then dml/, and orders a subfolder by its whole path",
            fix = "move the file into base/, ddl/ or dml/",
        )
    }

    @Test
    fun `every base migration runs again when it changes`() {
        // When
        val runOnce = baseMigrations.filterNot { migration -> migration.changeSets.all { it.isRunOnChange } }

        // Then
        runOnce shouldBreak rule(
            "base-runs-on-change",
            why = "base/ holds the schema, and runs again whenever it is edited or rebaselined",
            fix = "add `runOnChange:true` to the changeset",
        )
    }

    @Test
    fun `every base migration only creates what is missing`() {
        // When
        val changing = baseMigrations.filter { migration ->
            migration.statements.any { statement -> BASE_STATEMENTS.none { statement.startsWith(it) } }
        }

        // Then
        changing shouldBreak rule(
            "base-creates-only",
            why = "base/ runs again on existing databases, where each statement must change nothing " +
                "and block no read or write",
            fix = "keep to `SET`, `CREATE ... IF NOT EXISTS`, `CREATE INDEX CONCURRENTLY IF NOT EXISTS` and " +
                "`COMMENT ON`, and write any other change in a ddl/ file",
        )
    }

    @Test
    fun `every base migration is named after its level`() {
        // When
        val misnamed = baseMigrations.filterNot { it.fileName.matches(BASE_FILE_NAME) }

        // Then
        misnamed shouldBreak rule(
            "base-file-name",
            why = "the number orders the files of base/",
            fix = "name the file `<2-digit level>-<name in kebab case>.sql`",
        )
    }

    @Test
    fun `every ddl or dml migration runs once`() {
        // When
        val rerun = (ddlMigrations + dmlMigrations).filter { migration ->
            migration.changeSets.any { it.isRunOnChange }
        }

        // Then
        rerun shouldBreak rule(
            "run-once",
            why = "running the file again would take its locks again",
            fix = "remove `runOnChange`, and write any further change in a new file",
        )
    }

    @Test
    fun `every ddl or dml migration is named after its number`() {
        // When
        val misnamed = migrations.filter { it.folder in RUN_ONCE_FOLDERS && !it.fileName.matches(NUMBERED_FILE_NAME) }

        // Then
        misnamed shouldBreak rule(
            "numbered-file-name",
            why = "the number orders the files",
            fix = "name the file `<4-digit number>-<change in kebab case>.sql`",
        )
    }

    @Test
    fun `no two ddl or dml migrations of a folder share a number`() {
        // When
        val sharingNumbers = migrations.filter { it.folder in RUN_ONCE_FOLDERS }
            .groupBy { it.folder to it.fileName.substringBefore('-') }
            .values.filter { it.size > 1 }.flatten()

        // Then
        sharingNumbers shouldBreak rule(
            "unique-file-number",
            why = "two files with one number run in an order nobody chose",
            fix = "give the file the next free number of its folder",
        )
    }

    @Test
    fun `every index a ddl migration builds is dropped first`() {
        // When
        val keepingInvalidIndexes = ddlMigrations.filter { migration ->
            (listOf(Statement.NONE) + migration.statements).zipWithNext().any { (previous, statement) ->
                statement.indexBuiltConcurrently != null &&
                    statement.indexBuiltConcurrently != previous.indexDroppedConcurrently
            }
        }

        // Then
        keepingInvalidIndexes shouldBreak rule(
            "drop-index-before-build",
            why = "an interrupted build leaves an invalid index, which `IF NOT EXISTS` would then keep for good",
            fix = "precede the build with `DROP INDEX CONCURRENTLY IF EXISTS <schema>.<index>`",
        )
    }

    @Test
    fun `no ddl migration changes data`() {
        // When
        val changingData = ddlMigrations.filter { migration ->
            migration.statements.any { it.firstWord in DATA_CHANGES }
        }

        // Then
        changingData shouldBreak rule(
            "ddl-changes-no-data",
            why = "dml/ runs after ddl/, and data kept apart is reviewed and deployed on its own",
            fix = "move the `INSERT`, `UPDATE` or `DELETE` to a dml/ file",
        )
    }

    @Test
    fun `no dml migration changes the schema`() {
        // When
        val changingSchema = dmlMigrations.filter { migration ->
            migration.statements.any { it.firstWord !in DML_STATEMENTS }
        }

        // Then
        changingSchema shouldBreak rule(
            "dml-changes-no-schema",
            why = "a schema change in dml/ runs after the ddl/ files of its deployment and escapes their rules",
            fix = "move the statement to a ddl/ file",
        )
    }

    private fun rule(id: String, why: String, fix: String) =
        "Rule $id: $why. Fix: $fix. See \"Checked rules\" in docs/src/engineering/build-and-data/database.md."

    // Lists every file that breaks the rule, where shouldBeEmpty would name only the first one
    private infix fun List<Migration>.shouldBreak(rule: String) = withClue(
        rule,
    ) { map { it.path } shouldBe emptyList() }

    // A whole line of comment, so that no statement can follow a directive on its line
    private fun String.isComment() = trim().let { it.startsWith("--") || (it.startsWith("/*") && it.endsWith("*/")) }

    // `--changeset` and the other Liquibase directives have no space after the dashes
    private fun String.isReason() = trim().startsWith("-- ") && SQUAWK_DIRECTIVE !in this

    companion object {
        private val FOLDERS = listOf(BASE, DDL, DML)
        private val RUN_ONCE_FOLDERS = listOf(DDL, DML)
        private val TIMEOUTS = listOf("LOCK_TIMEOUT", "STATEMENT_TIMEOUT")
        private val BASE_FILE_NAME = Regex("""\d{2}-[a-z0-9-]+\.sql""")
        private val NUMBERED_FILE_NAME = Regex("""\d{4}-[a-z0-9-]+\.sql""")
        private val BASE_STATEMENTS = listOf(
            "SET",
            "CREATE SCHEMA IF NOT EXISTS",
            "CREATE TABLE IF NOT EXISTS",
            "CREATE SEQUENCE IF NOT EXISTS",
            "CREATE EXTENSION IF NOT EXISTS",
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS",
            "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS",
            "COMMENT ON",
        )
        private val DATA_CHANGES = listOf("INSERT", "UPDATE", "DELETE", "MERGE", "TRUNCATE", "COPY")
        private val DML_STATEMENTS = listOf("SET", "RESET", "INSERT", "UPDATE", "DELETE", "MERGE", "WITH", "SELECT")

        // Squawk reads a directive in any comment, after code or across lines: a line that mentions one is a directive
        private const val SQUAWK_DIRECTIVE = "squawk-"
    }
}
