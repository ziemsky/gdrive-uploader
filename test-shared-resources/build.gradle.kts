// todo check warnings "Runtime JAR files in the classpath have the version 1.2, which is older than the API version 1.3."

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("org.awaitility:awaitility-kotlin")

    implementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    implementation("io.github.microutils:kotlin-logging")
    implementation("ch.qos.logback:logback-classic")

    // Google Drive client
    implementation("com.google.oauth-client:google-oauth-client-jetty")
    implementation("com.google.apis:google-api-services-drive")
    implementation("com.google.api-client:google-api-client")

    implementation(project(":application"))
}
