plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("dev.inmo:krontab:0.7.1")
    api(project(":pleo-antaeus-models"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.1")
}