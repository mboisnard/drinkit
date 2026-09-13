// The root project holds what applies to the build as a whole rather than to any module — here, the IntelliJ settings
// https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html
plugins {
    id("com.drinkit.ide-convention")
    id("com.drinkit.code-analysis-convention")
}
