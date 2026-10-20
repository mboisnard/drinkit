package com.drinkit.architecture

import io.github.baole.konture.ClassDeclaration
import io.github.baole.konture.Konture
import io.github.baole.konture.SourceSetSelector
import io.github.baole.konture.SourceSets
import io.github.baole.konture.SourceUsage
import io.github.baole.konture.UsageKind
import io.github.baole.konture.isEnclosedInClass
import io.github.baole.konture.scope
import java.io.File

// Every helper the rules share. A comment starting "Konture lacks" names a gap of Konture 1.0.2 the helper works
// around; the other helpers hold a DrinkIt convention, which no library can know.

private val modules = Konture.projectGraph.getAllModules()
private val paths = modules.map { it.path }
private val files = modules.flatMap { it.files }

// Modules are found by their name, never by their folder, so that a new, renamed or moved one stays checked: an
// application's modules start with its name and end in -domain, -infra or -backend, and a tech starter in -starter
private fun String.name() = substringAfterLast(':')

private fun endingWith(suffix: String) = paths.filter { it.name().endsWith(suffix) }

internal val DOMAINS = endingWith("-domain")
internal val INFRAS = endingWith("-infra")
internal val BACKENDS = endingWith("-backend")
internal val STARTERS = endingWith("-starter")

private val applicationNames = (DOMAINS + INFRAS + BACKENDS).map { it.name().substringBeforeLast('-') }.toSet()

internal fun applicationOf(module: String): String? =
    applicationNames.filter { module.name().startsWith("$it-") }.maxByOrNull { it.length }

internal val APPLICATIONS = paths.filter { it !in STARTERS && applicationOf(it) != null }
internal val ALL = APPLICATIONS + STARTERS

// The root package of an application is the one of the @SpringBootApplication of its backend, and a bounded context is
// the package right under it: com.drinkit, then cellar. No rule names the root
private val applicationRoots: Map<String, String> by lazy {
    BACKENDS.associate { backend ->
        val applications = classesOf(listOf(backend)).filter { cls ->
            cls.annotations.any { it.name == "SpringBootApplication" }
        }
        check(applications.size == 1) { "$backend must hold one @SpringBootApplication, the root of its application" }
        applicationOf(backend).orEmpty() to applications.single().packageName
    }
}

// The root of a package: the longest part it shares with an application root, com.drinkit for com.drinkit.cellar and
// for com.drinkit.security, the package of a tech starter
internal fun rootPackageOf(name: String): String {
    val segments = name.split('.')
    return applicationRoots.values
        .map { root -> root.split('.').asSequence().zip(segments.asSequence()).takeWhile { (a, b) -> a == b }.toList() }
        .maxBy { it.size }
        .joinToString(".") { it.first }
}

private val starterPackages by lazy { classesOf(STARTERS).map { it.packageName }.toSet() }

// A name of a tech starter, which every application may use: a class or a function of one of its packages
internal fun String.isOfStarter() = starterPackages.any { starterPackage ->
    val rest = removePrefix("$starterPackage.")
    rest != this && ('.' !in rest || rest[0].isUpperCase())
}

internal fun contextOf(name: String): String {
    val root = rootPackageOf(name)
    val context = name.removePrefix(root).removePrefix(".").substringBefore('.')
    return if (context.isEmpty()) root else "$root.$context"
}

// A domain module without code yet has no slice to check
internal val domainsWithCode by lazy { DOMAINS.filter { classesOf(listOf(it)).isNotEmpty() } }

// Konture lacks a slice pattern with a wildcard before its capture, *.(*).., so a pattern starts with the root package
internal fun rootOf(domain: String) = applicationRoots[applicationOf(domain)]
    ?: error("$domain has no backend module holding a @SpringBootApplication, which gives its root package")

// The source sets of these modules, production by default
internal fun sourceSetsOf(modules: List<String>, sourceSets: SourceSetSelector = SourceSets.production()) =
    sourceSets and modules.fold(SourceSets.named()) { selected, module -> selected or SourceSets.inModule(module) }

internal fun classesOf(modules: List<String>, sourceSets: SourceSetSelector = SourceSets.production()) =
    Konture.scope(sourceSetsOf(modules, sourceSets)).classes

// Konture lacks a link from a class to its file, which holds its usages and its source sets
internal fun ClassDeclaration.file() = files.first { it.filePath == filePath }

private val moduleOfFile: Map<String, String> by lazy {
    modules.flatMap { module -> module.files.map { it.filePath to module.path } }.toMap()
}

// Konture lacks a link from a class to its module
internal fun ClassDeclaration.module() = moduleOfFile.getValue(filePath)

// Konture lacks the usages of one class: they hang off its file, imports included, which belong to no class
internal fun ClassDeclaration.usages(): List<SourceUsage> = file().usages
    .filter { it.isEnclosedInClass(fqName) }
    .filter { it.enclosingClass != null || it.enclosingFunction != null || it.enclosingProperty != null }

// Konture lacks full names in referencedTypes, which mixes them with simple ones: its class references have them
internal fun ClassDeclaration.referencedTypeNames(): Set<String> =
    usages().filter { it.kind == UsageKind.CLASS_REFERENCE }.map { it.targetFqName }.toSet()

// Konture lacks the full name of a supertype of the same package, which needs no import: only a that() selection
// resolves it, through areAssignableTo, and a rule checking one class against another has none
internal fun ClassDeclaration.extends(other: ClassDeclaration) =
    other.fqName in supertypes || (other.name in supertypes && packageName == other.packageName)

// Konture lacks the arguments of a call: it records now() and now(clock) as the same call, so the text tells them apart
internal fun SourceUsage.callsNowWithoutClock() =
    kind == UsageKind.CALL && targetFqName.matches(Regex("""java\.time\.\w+\.now""")) &&
        File(filePath).readText().substring(sourceStartOffset, sourceEndOffset) == "now()"

// Konture lacks a selection of classes by the annotations of their functions: an HTTP client declares its requests
// with @GetExchange, @PostExchange or another @*Exchange
internal fun ClassDeclaration.isHttpClient() =
    functions.any { function -> function.annotations.any { it.name.endsWith("Exchange") } }

// A package of a bounded context has a role: the use cases and what sits beside them, such as commands and deciders,
// at its root, the entities and values in core, the ports in spi. What the contexts share sits in common
internal enum class Role { USE_CASES, CORE, SPI, COMMON }

internal fun ClassDeclaration.role() = when {
    contextOf(packageName).endsWith(".common") -> Role.COMMON
    ".core." in "$packageName." -> Role.CORE
    ".spi." in "$packageName." -> Role.SPI
    else -> Role.USE_CASES
}

private val domainClasses: Map<String, ClassDeclaration> by lazy { classesOf(DOMAINS).associateBy { it.fqName } }

// The classes of the domain that a class uses: the other names are libraries and starters
internal fun ClassDeclaration.referencedDomainClasses(): List<ClassDeclaration> =
    referencedTypeNames().mapNotNull { domainClasses[it] }

// A port is an interface of the domain in a spi package, or one infra implements; an adapter is a class of infra that
// implements a port. codebase.package-has-one-owner keeps two applications from sharing a full name
internal val ports: List<ClassDeclaration> by lazy {
    val implemented = classesOf(INFRAS).flatMap { it.supertypes }.toSet()
    classesOf(DOMAINS).filter { it.isInterface && (".spi." in "${it.packageName}." || it.fqName in implemented) }
}

internal val portNames by lazy { ports.map { it.fqName }.toSet() }

internal val adapters: Map<ClassDeclaration, List<ClassDeclaration>> by lazy {
    classesOf(INFRAS).associateWith { adapter -> ports.filter { it.fqName in adapter.supertypes } }
        .filterValues { it.isNotEmpty() }
}

// An adapter is named after a technology or a source, then its port or the port's last word: JooqCellars,
// EuropeanCentralBankProvider
internal fun String.namesPort(port: String): Boolean {
    val lastWord = port.substring(port.indexOfLast { it.isUpperCase() })
    return listOf(port, lastWord).any { length > it.length && endsWith(it) }
}

// Konture lacks meta-annotations: it misses that @JooqRepository carries @Transactional(propagation = MANDATORY), so
// the rules find the starter annotations that do, then the adapters that carry one, then their ports
internal val transactionalPorts by lazy {
    val demandingAnnotations = classesOf(STARTERS).filter { it.demandsTransaction() }.map { it.name }.toSet()
    adapters
        .filterKeys { adapter ->
            adapter.demandsTransaction() ||
                adapter.annotations.any { it.name in demandingAnnotations }
        }
        .values.flatten().toSet()
}

private fun ClassDeclaration.demandsTransaction() = annotations.any { annotation ->
    annotation.name == "Transactional" && annotation.arguments.any { it.value.endsWith("MANDATORY") }
}

// The bounded contexts of each application, common included: com.drinkit.cellar for a class of drinkit-domain
internal val contextsByApplication: Map<String?, Set<String>> by lazy {
    DOMAINS.associate { domain ->
        applicationOf(domain) to classesOf(listOf(domain)).map { contextOf(it.packageName) }.toSet()
    }
}

internal val contexts: Set<String> by lazy { contextsByApplication.values.flatten().toSet() }
