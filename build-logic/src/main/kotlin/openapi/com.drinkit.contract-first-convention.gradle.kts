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
// (see `openApiInput(project(...))` in consuming modules), `resolvable` is what actually gets resolved, the variant
// its category selects. https://docs.gradle.org/current/userguide/declaring_configurations.html
val openApiInput = configurations.dependencyScope("openApiInput")
val openApiInputPath = configurations.resolvable("openApiInputPath") {
    extendsFrom(openApiInput)
    attributes { attribute(Category.CATEGORY_ATTRIBUTE, objects.named(OPENAPI_CONTRACT_CATEGORY)) }
}

val openApiCommonTemplates = configurations.dependencyScope("openApiCommonTemplates")
val openApiCommonTemplatesPath = configurations.resolvable("openApiCommonTemplatesPath") {
    extendsFrom(openApiCommonTemplates)
    attributes { attribute(Category.CATEGORY_ATTRIBUTE, objects.named(OPENAPI_TEMPLATES_CATEGORY)) }
}

val generatedSources = layout.buildDirectory.dir("generated-sources/openapi/src/main/kotlin")

// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
openApiGenerate {
    generatorName.set("kotlin-spring")
    outputDir.set(generatedSources)

    // Remove old generated files before starting a new generation task
    cleanupOutput.set(true)

    // Options for code generation depending on the generator used
    // https://openapi-generator.tech/docs/generators/kotlin-spring/
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true", // Generates the mapped interfaces a controller implements, not the controllers
            "skipDefaultInterface" to "true", // An operation left unimplemented fails the compilation, not answers 501
            "useTags" to "true", // Use the defined tags in the yaml file to create interfaces and class names
            "useSpringBoot3" to "true", // Jakarta annotations in the generated code
            "sourceFolder" to "",
            "enumPropertyNaming" to "UPPERCASE",
        ),
    )

    // Scope the generated files to only apis/models classes
    // Can also be configured in .openapi-generator-ignore file
    // https://openapi-generator.tech/docs/globals/
    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
        ),
    )

    additionalProperties.set(
        mapOf(
            "removeEnumValuePrefix" to "false", // Disable enum stripping on generated code
        ),
    )
}

tasks.openApiGenerate {
    // Resolved only when the task needs it, never while the build is configured
    inputSpec.fileProvider(
        openApiInputPath.flatMap { it.elements }.map { it.single().asFile.resolve("api-definition.yaml") },
    )

    inputs.files(openApiInputPath)
    inputs.files(openApiCommonTemplatesPath)
}

kotlin {
    sourceSets {
        main {
            // The folder rather than the task: reading the sources then never creates the task, which the
            // module graph of the architecture tests does from the root project
            kotlin.srcDir(files(generatedSources).builtBy(tasks.openApiGenerate))
        }
    }
}
