
plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    mavenCentral()
    jcenter()
}

gradlePlugin {
    plugins {
        create("git-semver-release-plugin") {
            id = "com.ziemsky.gradle.git-semver-release-plugin"
            implementationClass = "com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleasePlugin"
        }
    }
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:5.8.0.202006091008-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.8.0.202006091008-r")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.1.0")
    testImplementation("io.kotest:kotest-property-jvm:4.1.0")
    testImplementation("io.kotest:kotest-extensions-spring:4.1.0")
    testImplementation("io.kotest:kotest-runner-console-jvm:4.1.0")
}

tasks {
    val test by getting(Test::class) {
        useJUnitPlatform()
    }
}
