plugins {
    kotlin("jvm")
}

val useApi = providers.gradleProperty("useApi").map(String::toBoolean).getOrElse(false)

dependencies {
    if (useApi) {
        api(project(":value-class"))
    } else {
        implementation(project(":value-class"))
    }
}
