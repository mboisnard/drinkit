package com.drinkit.architecture

import io.github.baole.konture.ClassDeclaration
import io.github.baole.konture.FileDeclaration
import io.github.baole.konture.Konture
import io.github.baole.konture.SourceSetSelector
import io.github.baole.konture.SourceSets
import io.github.baole.konture.SourceUsage
import io.github.baole.konture.UsageKind
import io.github.baole.konture.classScopeFromModule
import io.github.baole.konture.isEnclosedInClass

// What the rules need beyond Konture's assertions: the names a file or a class uses, what a module declares, and the
// roles DrinkIt gives its classes: port, adapter, controller
private val modules = Konture.projectGraph.getAllModules()
private val files = modules.flatMap { it.files }

internal fun classesOf(modules: List<String>, sourceSets: SourceSetSelector = SourceSets.production()) =
    modules.flatMap { Konture.classScopeFromModule(it, sourceSets).classes }

internal fun ClassDeclaration.file() = files.first { it.filePath == filePath }

internal fun ClassDeclaration.module() = modules.first { module -> module.files.any { it.filePath == filePath } }.path

// The names a file imports, calls or refers to. Konture leaves a name of the same package, which needs no import,
// unqualified: it is qualified here
internal fun FileDeclaration.namesUsed(): Set<String> =
    (imports + usages.map { it.targetFqName }).map { if ('.' in it) it else "$packageName.$it" }.toSet()

// An import has no enclosing declaration, and belongs to no class of its file
internal fun ClassDeclaration.usages(): List<SourceUsage> = file().usages
    .filter { it.isEnclosedInClass(fqName) }
    .filter { it.enclosingClass != null || it.enclosingFunction != null || it.enclosingProperty != null }

internal fun ClassDeclaration.referencedTypeNames(): Set<String> =
    usages().filter { it.kind == UsageKind.CLASS_REFERENCE }.map { it.targetFqName }.toSet()

// What the production code of these modules declares, classes and top-level functions and properties, by full name
internal fun declarationsOf(modulePaths: List<String>): Set<String> = modules.asSequence()
    .filter { it.path in modulePaths }
    .flatMap { it.files }
    .filter { file -> file.sourceSets.any { it.name == "main" } }
    .flatMap { file -> file.classes.map { it.fqName } + file.topLevelNames() }
    .toSet()

private fun FileDeclaration.topLevelNames() =
    (topLevelFunctions.map { it.name } + topLevelProperties.map { it.name }).map { "$packageName.$it" }

internal val ports: List<ClassDeclaration> by lazy {
    val implemented = classesOf(INFRAS).flatMap { it.supertypes }.toSet()
    classesOf(DOMAINS).filter { it.isInterface && (".spi." in "${it.packageName}." || it.fqName in implemented) }
}

internal val adapters: Map<ClassDeclaration, List<ClassDeclaration>> by lazy {
    classesOf(INFRAS).associateWith { adapter -> ports.filter { it.fqName in adapter.supertypes } }
        .filterValues { it.isNotEmpty() }
}

internal val controllers: List<ClassDeclaration> by lazy {
    classesOf(BACKENDS).filter { cls -> cls.supertypes.any { it.endsWith("ApiDelegate") } }
}

// The bounded contexts of each application, common included: com.drinkit.cellar for a class of drinkit-domain
internal val contextsByApplication: Map<String?, Set<String>> by lazy {
    DOMAINS.associate { domain ->
        applicationOf(domain) to classesOf(listOf(domain)).map { contextOf(it.packageName) }.toSet()
    }
}

internal val contexts: Set<String> by lazy { contextsByApplication.values.flatten().toSet() }
