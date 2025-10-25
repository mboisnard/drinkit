plugins {
    id("com.drinkit.common-convention")
    id("org.openapi.generator")
}

dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
}

val openApiInput: Configuration by configurations.creating {
    isCanBeConsumed = false
}

val openApiCommonTemplates: Configuration by configurations.creating {
    isCanBeConsumed = false
}

// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
openApiGenerate {
    generatorName.set("kotlin-spring")
    outputDir.set(layout.buildDirectory.dir("generated-sources/openapi/src/main/kotlin").map { it.asFile.toString() })

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
    if (openApiInput.files.isNotEmpty()) {
        inputSpec.set("${openApiInput.singleFile.path}/api-definition.yaml")
    }

    inputs.files(openApiInput)
    inputs.files(openApiCommonTemplates)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(tasks.openApiGenerate)
        }
    }
}