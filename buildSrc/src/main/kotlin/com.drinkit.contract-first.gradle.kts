plugins {
    id("org.openapi.generator")
    id("com.drinkit.common-conventions")
}

val openApiInput: Configuration by configurations.creating {
    isCanBeConsumed = false
}

// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("${openApiInput.singleFile.path}/api-definition.yaml")
    outputDir.set(layout.buildDirectory.dir("generated-sources/openapi/src/main/kotlin").map { it.asFile.toString() })
    cleanupOutput.set(true)
    configOptions.set(
        mapOf(
            "delegatePattern" to "true",
            "useTags" to "true",
            "sourceFolder" to "",
            "enumPropertyNaming" to "UPPERCASE"
        )
    )
    typeMappings.set(
        mapOf(
            "string+bigdecimal" to "BigDecimal"
        )
    )
    importMappings.set(
        mapOf(
            "BigDecimal" to "java.math.BigDecimal"
        )
    )
    additionalProperties.set(
        mapOf(
            "removeEnumValuePrefix" to false
        )
    )
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(tasks.openApiGenerate)
        }
    }
}