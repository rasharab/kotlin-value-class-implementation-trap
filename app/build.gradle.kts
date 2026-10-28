plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":library"))
}

application {
    mainClass = "demo.app.MainKt"
}
