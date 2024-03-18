import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.5")
}

detekt {
    source.setFrom("src/main/kotlin", "src/test/kotlin")
    parallel = true
    buildUponDefaultConfig = true
    //autoCorrect = true
    config.setFrom("$rootDir/code-analysis/detekt/detekt.yml")
    baseline = file("$rootDir/code-analysis/detekt/baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "1.8"
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"
}