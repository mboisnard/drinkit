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

fun addGradleProject(folder: File) {
    include(folder.name)
    project(":${folder.name}").projectDir = folder.absoluteFile
}

fun containsAGradleFile(folder: File): Boolean {
    if (!folder.isDirectory) {
        return false
    }

    return folder.listFiles()
        ?.any { it.name.equals("build.gradle.kts") } == true
}

fun includeModules(folder: File)  {
    if (!folder.isDirectory || folder.isHidden) {
        return
    }

    if (containsAGradleFile(folder)) {
        addGradleProject(folder)
        return
    }

    folder.listFiles()
        ?.filter { it.isDirectory && !it.isHidden }
        ?.filter { !excludedFolders.contains(it.name) }
        ?.filter { containsAGradleFile(it) }
        ?.forEach { addGradleProject(it) }
}

includeModules(file("tech-starters/backend"))
includeModules(file("drinkit"))
includeModules(file("deployment/updater"))