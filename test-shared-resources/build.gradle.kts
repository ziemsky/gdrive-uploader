// todo check warnings "Runtime JAR files in the classpath have the version 1.2, which is older than the API version 1.3."

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core") {
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation("org.awaitility:awaitility-kotlin") {
        exclude(group = "org.jetbrains.kotlin")
    }

    implementation(files("../lib/fs-structure-0.1.0-SNAPSHOT.jar"))

    implementation("io.github.microutils:kotlin-logging") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.slf4j")
    }
    implementation("ch.qos.logback:logback-classic")

    // Google Drive client
    implementation("com.google.oauth-client:google-oauth-client-jetty")
    implementation("com.google.apis:google-api-services-drive") {
        exclude(group = "com.fasterxml.jackson.core")
    }
    implementation("com.google.api-client:google-api-client") {
        exclude(group = "com.fasterxml.jackson.core")
    }

    implementation(project(":application"))
}
