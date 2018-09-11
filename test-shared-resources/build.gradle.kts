
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("com.github.tomakehurst:wiremock:2.18.0")

    implementation("org.awaitility:awaitility-kotlin:3.1.2")
}
