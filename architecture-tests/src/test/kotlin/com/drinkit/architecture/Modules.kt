package com.drinkit.architecture

import io.github.baole.konture.Konture
import io.github.baole.konture.ProjectGraph

// Modules are found by their name, never by their folder, so that a new, renamed or moved one stays checked: an
// application's modules start with its name and end in -domain, -infra or -backend, and a tech starter in -starter
private val paths = Konture.projectGraph.getAllModules().map { it.path }

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

// A package starts with a root of two segments, its organisation's reversed domain, then its bounded context:
// com.drinkit, then cellar. No rule names the root, so an application of another organisation is checked too
internal fun rootPackageOf(name: String) = name.split('.').take(2).joinToString(".")

internal fun contextOf(name: String) = name.split('.').take(3).joinToString(".")

// A slice takes the classes of its packages from every module of the graph it reads, so a rule on the slices of one
// module reads a graph of that module alone
internal fun graphOf(module: String) =
    ProjectGraph(mapOf(":" to Konture.projectGraph.getAllModules().filter { it.path == module }))
