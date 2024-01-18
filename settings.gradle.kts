rootProject.name = "drinkit"

// Multi project info: https://docs.gradle.org/current/userguide/intro_multi_project_builds.html#sec:project_standard
// Create a dynamic include logic to avoid adding any new submodule here
include("drinkit-api-contract")
include("drinkit-backend")
include("drinkit-domain")
include("drinkit-infra")

include("upgrader")
project(":upgrader").projectDir = file("deployment/upgrader")