rootProject.name = "drinkit"

// Version catalog location is configured here to be able to use it in all subprojects
// By default we don't need to configure location but we decided to group files in the platform module
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./gradle/platform/libs.versions.toml"))
        }
    }
}


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

fun includeModulesFrom(folder: File) {
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

addGradleProject(file("gradle/platform"))
addGradleProject(file("deployment/updater"))

includeModulesFrom(file("tech-starters/backend"))
includeModulesFrom(file("drinkit"))