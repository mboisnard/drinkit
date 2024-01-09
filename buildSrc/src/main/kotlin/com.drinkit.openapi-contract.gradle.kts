val openApi: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

val openApiTemplates: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(openApi.name, file(layout.projectDirectory.dir("contract")))
    add(openApiTemplates.name, file(layout.projectDirectory.dir("templates")))
}