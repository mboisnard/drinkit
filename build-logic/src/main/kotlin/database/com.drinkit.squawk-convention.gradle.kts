// Add-on convention: Squawk, the PostgreSQL migration linter, checks the SQL changelogs of the module on `check`.
// Gradle downloads its native binary from the GitHub release and runs it by its path: no Node, no PATH lookup
plugins {
    base
}

val changelogs = "src/main/resources/db/changelog"
val squawkLibrary = versionCatalogs.named("libs").findLibrary("squawk").get()

// The release assets: squawk-<platform>, with .exe on Windows
val squawkPlatforms = listOf("darwin-arm64", "darwin-x64", "linux-arm64", "linux-x64", "windows-x64")
val osName = providers.systemProperty("os.name")
val executableName = osName.map { if (it.startsWith("Windows")) "squawk.exe" else "squawk" }

val squawkPlatform = osName.zip(providers.systemProperty("os.arch")) { os, arch ->
    val system = mapOf("Mac" to "darwin", "Linux" to "linux", "Windows" to "windows")[os.substringBefore(' ')]
    val cpu = mapOf("aarch64" to "arm64", "amd64" to "x64", "x86_64" to "x64")[arch]
    "$system-$cpu".takeIf { it in squawkPlatforms }
        ?: throw GradleException("Squawk has no binary for $os on $arch, only for ${squawkPlatforms.joinToString()}")
}

fun DependencyHandler.squawkBinaryOf(platform: String) = variantOf(squawkLibrary) {
    classifier(platform)
    artifactType(if (platform.startsWith("windows")) "exe" else "")
}

// The platform is read when the binary is resolved, not when a plugin such as Konture lists the declared
// dependencies: an unsupported platform fails the squawk task alone, never the whole build
val squawkTool = configurations.dependencyScope("squawkTool") {
    withDependencies {
        val handler = project.dependencies
        add(handler.create(handler.squawkBinaryOf(squawkPlatform.get()).get()))
    }
}
val nativeRuntime = objects.named<Usage>(Usage.NATIVE_RUNTIME)
val squawkBinary = configurations.resolvable("squawkBinary") {
    extendsFrom(squawkTool)
    attributes { attribute(Usage.USAGE_ATTRIBUTE, nativeRuntime) }
}

// Never resolved by a task. Writing the verification metadata resolves every configuration, so this one puts the
// checksum of every platform's binary in gradle/verification-metadata.xml, whatever machine writes it
val squawkEveryPlatform = configurations.dependencyScope("squawkEveryPlatform")
configurations.resolvable("squawkEveryPlatformBinaries") {
    extendsFrom(squawkEveryPlatform)
    attributes { attribute(Usage.USAGE_ATTRIBUTE, nativeRuntime) }
}

dependencies {
    squawkPlatforms.forEach { add(squawkEveryPlatform.name, squawkBinaryOf(it)) }
}

val squawkDirectory = layout.buildDirectory.dir("squawk/bin")

val installSquawk = tasks.register<Copy>("installSquawk") {
    description = "Copies the Squawk binary of this platform, executable, under build/squawk/bin"
    val name = executableName
    val directory = squawkDirectory
    from(squawkBinary)
    into(directory)
    rename { name.get() }
    filePermissions { unix("rwxr-xr-x") }
    // A new file rather than the old one overwritten: macOS kills a signed binary that changed in place
    doFirst { directory.get().asFile.deleteRecursively() }
}

val squawk = tasks.register<Exec>("squawk") {
    description = "Lints the SQL of the changelogs with Squawk, as .squawk.toml configures it"
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    workingDir = layout.projectDirectory.asFile
    val binary = squawkDirectory.zip(executableName) { directory, name -> directory.file(name).asFile.path }
    // executable takes a String, not a Provider: set it once the task runs
    doFirst { (this as Exec).executable = binary.get() }
    args("$changelogs/**/*.sql")
    inputs.files(installSquawk)
    inputs.file(".squawk.toml")
    inputs.dir(changelogs)
    val passed = layout.buildDirectory.file("squawk/passed")
    outputs.file(passed)

    // Squawk exits 1 once it has printed its findings or a configuration error: say what to do next instead of an
    // exit code. Any other exit value means it never checked the changelogs
    isIgnoreExitValue = true
    val nextStep = "Squawk failed on the changelogs, as printed above. A finding names its rule, links to its page " +
        "and shows a fix, and a configuration error names its line in .squawk.toml. The section \"Checked rules\" " +
        "of docs/src/engineering/build-and-data/database.md says why each rule matters. When a rule does not apply " +
        "to a statement, write `-- squawk-ignore <rule>` on its own line, right below a `-- <reason>` comment."
    doLast {
        when (val exitValue = executionResult.get().exitValue) {
            0 -> passed.get().asFile.writeText("")

            1 -> throw GradleException(nextStep)

            else -> throw GradleException(
                "Squawk stopped with exit value $exitValue before it checked the changelogs. " +
                    "Run ${binary.get()} --version to see why.",
            )
        }
    }
}

tasks.check {
    dependsOn(squawk)
}
