val openApi = configurations.consumable("openApi")
val openApiTemplates = configurations.consumable("openApiTemplates")

artifacts {
    add(openApi.name, file(layout.projectDirectory.dir("contract")))
    add(openApiTemplates.name, file(layout.projectDirectory.dir("templates")))
}
