
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("com.github.tomakehurst:wiremock:2.18.0")

    implementation("org.awaitility:awaitility-kotlin:3.1.2")

    implementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    implementation("io.github.microutils:kotlin-logging:1.6.10")

    // Google Drive client
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.25.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev130-1.25.0")
    implementation("com.google.api-client:google-api-client:1.25.0")
}
