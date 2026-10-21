val openApi = configurations.consumable("openApi") {
    attributes { attribute(Category.CATEGORY_ATTRIBUTE, objects.named(OPENAPI_CONTRACT_CATEGORY)) }
}
val openApiTemplates = configurations.consumable("openApiTemplates") {
    attributes { attribute(Category.CATEGORY_ATTRIBUTE, objects.named(OPENAPI_TEMPLATES_CATEGORY)) }
}

artifacts {
    add(openApi.name, file(layout.projectDirectory.dir("contract")))
    add(openApiTemplates.name, file(layout.projectDirectory.dir("templates")))
}
