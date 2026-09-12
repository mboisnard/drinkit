// Add-on convention: only what the OpenAPI generation below needs. The module-wide setup — platform,
// toolchain, Spring plugins — comes from the api-convention this is always combined with.
plugins {
    kotlin("jvm")
    id("org.openapi.generator")
}

dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
}

// Gradle role-based configurations: `dependencyScope` receives the declared dependencies
// (see `openApiInput(project(...))` in consuming modules), `resolvable` is what actually gets resolved
// https://docs.gradle.org/current/userguide/declaring_configurations.html
val openApiInput = configurations.dependencyScope("openApiInput")
val openApiInputPath = configurations.resolvable("openApiInputPath") {
    extendsFrom(openApiInput.get())
}

val openApiCommonTemplates = configurations.dependencyScope("openApiCommonTemplates")
val openApiCommonTemplatesPath = configurations.resolvable("openApiCommonTemplatesPath") {
    extendsFrom(openApiCommonTemplates.get())
}

// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
openApiGenerate {
    generatorName.set("kotlin-spring")
    outputDir.set(layout.buildDirectory.dir("generated-sources/openapi/src/main/kotlin"))

    // Remove old generated files before starting a new generation task
    cleanupOutput.set(true)

    // Options for code generation depending on the generator used
    // https://openapi-generator.tech/docs/generators/kotlin-spring/
    configOptions.set(
        mapOf(
            "delegatePattern" to "true", // Delegate pattern will create an interface that we can easily implement with a Spring service
            "useTags" to "true", // Use the defined tags in the yaml file to create interfaces and class names
            "useSpringBoot3" to "true", // Use jakarta annotations in generated code, jakarta libraries are imported by spring boot dependencies
            "sourceFolder" to "",
            "enumPropertyNaming" to "UPPERCASE",
        )
    )

    // Scope the generated files to only supportingFile/apis/models classes
    // Can also be configured in .openapi-generator-ignore file
    // https://openapi-generator.tech/docs/globals/
    globalProperties.set(
        mapOf(
            "supportingFiles" to "ApiUtil.kt",
            "apis" to "",
            "models" to "",
        )
    )

    additionalProperties.set(
        mapOf(
            "removeEnumValuePrefix" to "false", // Disable enum stripping on generated code
        )
    )
}

tasks.openApiGenerate {
    val openApiInputFiles = openApiInputPath.get()
    if (openApiInputFiles.files.isNotEmpty()) {
        inputSpec.set("${openApiInputFiles.singleFile.path}/api-definition.yaml")
    }

    inputs.files(openApiInputPath)
    inputs.files(openApiCommonTemplatesPath)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(tasks.openApiGenerate)
        }
    }
}