rootProject.name = "drinkit"

// Gradle Multi projects
// https://docs.gradle.org/current/userguide/intro_multi_project_builds.html#sec:project_standard
// Here we dynamically includes all gradle subprojects from specified folders

val excludedFolders = setOf(
    "build",
    "node_modules",
    "out",
    "buildSrc",
)
val registeredProjectFolders = mutableSetOf<File>()
fun includeModules(folder: File, depth: Int) {
    if (depth > 2 || folder in registeredProjectFolders) {
        return
    }

    val children = folder.listFiles()

    if (children?.any { it.name.equals("build.gradle.kts") } == true) {
        include(folder.name)
        project(":${folder.name}").projectDir = folder.absoluteFile
        registeredProjectFolders.add(folder)
    }

    children?.filter { it.isDirectory && !it.isHidden }
        ?.filter { !excludedFolders.contains(it.name) }
        ?.forEach { includeModules(it, depth + 1) }
}

includeModules(file("starters"), 1)
includeModules(file("drinkit"), 1)
includeModules(file("deployment/updater"), 2)