dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
    versionCatalogs {
        create("pluginLibs") {
            from(files("../gradle/pluginLibs.versions.toml"))
        }
    }
}
