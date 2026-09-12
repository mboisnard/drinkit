rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal() // so that external plugins can be resolved in the dependencies section
        mavenCentral()
    }

    versionCatalogs {
        // Catalogs live in the platform module, hence the explicit location
        create("libs") {
            from(files("../gradle/platform/libs.versions.toml"))
        }
        create("pluginLibs") {
            from(files("../gradle/platform/pluginLibs.versions.toml"))
        }
    }
}
