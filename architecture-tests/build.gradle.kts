import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("com.drinkit.common-convention")
    id("com.drinkit.architecture-tests-convention")
}

dependencies {
    testImplementation(libs.konture)
}

// A broken rule prints its id, its reason and the fix in the console, not only in the test report
tasks.test {
    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
    }
}
