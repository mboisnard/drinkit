// Commented out: on Gradle 9.8.0, name.remal.idea-settings 4.0.9 makes every build that stores the configuration
// cache exit 1 without output. Restore it and apply it from the root build once a fixed plugin version is out.
/*import name.remal.gradle_plugins.idea_settings.IdeaRunOnSaveSettings.ReformatMode

plugins {
    id("name.remal.idea-settings")
}

// .idea is gitignored, so IDE settings are declared here and IntelliJ regenerates them at each sync.
// The plugin also enables EditorConfig support, fixes .properties encoding and delegates Build/Run
// actions to Gradle, with no DSL.
ideaSettings {
    requiredPlugins.add("detekt")

    runOnSave {
        // IntelliJ's formatter only reaches KOTLIN_OFFICIAL, which is one notch below the
        // `ktlint_official` detekt enforces: reformatting on save would undo the wrapped signatures
        // on every save. Formatting stays detekt's, through the pre-commit hook.
        reformatMode = ReformatMode.DISABLED
        optimizeImports = true
    }

    database {
        defaultDialect = "PostgreSQL"
    }
}

// gradle-idea-ext holds a Project reference in this task, which the configuration cache cannot
// serialise. Opting out degrades it to a cache miss instead of failing the build; the task is
// registered late, hence the lazy matching.
tasks.matching { it.name == "processIdeaSettings" }.configureEach {
    notCompatibleWithConfigurationCache("org.jetbrains.gradle.ext keeps a Project reference in the task")
}
*/
