// Konture, applied to the root project, which builds the module graph, and to architecture-tests, which reads it.
// Not from the settings: every module's tests would depend on a graph that changes with any source file
plugins {
    id("io.github.baole.konture")
}

konture {
    // The jOOQ classes and the OpenAPI interfaces are generated: no rule applies to them
    excludePackages("..generated..")
}

// Konture 1.0.2 lists every module's sources at configuration time to find rules on external libraries, which none of
// ours is: without this, any Kotlin edit discards the configuration cache
tasks.withType<io.github.baole.konture.plugin.DetectExternalDependencyRules>().configureEach {
    testSources.setFrom()
}
