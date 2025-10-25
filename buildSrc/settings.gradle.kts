dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/platform/libs.versions.toml"))
        }
    }
    versionCatalogs {
        create("pluginLibs") {
            from(files("../gradle/platform/pluginLibs.versions.toml"))
        }
    }
}
