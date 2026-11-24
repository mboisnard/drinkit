plugins {
    id("com.drinkit.library-convention")
}

dependencies {
    // @JsonValue on AbstractId, at compile time only: not passed on to its users,
    // so the domain neither carries Jackson nor can use its annotations
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation("tools.jackson.module:jackson-module-kotlin")
}
