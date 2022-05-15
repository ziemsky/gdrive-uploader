import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleaseFullTask
import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleaseTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Paths

plugins {
    java
    kotlin("jvm") apply false

    id("io.spring.dependency-management")
    id("com.dorongold.task-tree")
    id("com.github.ben-manes.versions")

    id("com.ziemsky.gradle.git-semver-release-plugin")
}

// todo document uploader.run.environment in readme
val defaultRunEnvironment = "local"
if (!project.hasProperty("uploader.run.environment")) {
    project.extra.set("uploader.run.environment", defaultRunEnvironment)
}

if (!project.hasProperty("conf.path")) {
    val envSpecificConfDirPath = Paths.get(
            project.projectDir.absolutePath,
            "conf",
            project.properties["uploader.run.environment"] as String
    ).toString()
    project.extra.set("conf.path", envSpecificConfDirPath)
}

// In case of a conflict of dependency versions (multiple versions of the same dependency detected),
// Gradle by default uses the newest of conflicting versions. The following config can influence
// this behaviour.
// Note that io.spring.dependency-management plugin doesn't seem to support excluding transitive dependencies,
// so if resolutionStrategy.failOnVersionConflict() flags conflicts, the offending (older) dependency need excluding.
// Offending transitive dependencies have to be then excluded at the dependency level in individual
// sub-projects rather than here.
//
// Best to rely on Gradle's default resolution and keep this commented out, otherwise exclusions
// have to be managed manually for no clear gain, and there is normally A LOT of them required.
//
//allprojects {
//    configurations.all { resolutionStrategy.failOnVersionConflict() }
//}

// defined in gradle.properties; done this way to support referencing from buildscript
val awaitilityVersion: String by rootProject
val kotlinVersion: String by rootProject
val kotlinCoroutinesVersion: String by rootProject
val springBootVersion: String by rootProject
val springVersion: String by rootProject
val springIntegrationVersion: String by rootProject

val gDriveVersion = "1.27.0"

subprojects {
    repositories {
        mavenCentral()
        jcenter()
    }

    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencyManagement {

        dependencies {
            dependencySet("org.jetbrains.kotlin:$kotlinVersion") { // todo tie to the one in settings file
                entry("kotlin-stdlib-jdk8")
                entry("kotlin-stdlib")
                entry("kotlin-reflect")
            }

            dependencySet("org.jetbrains.kotlinx:$kotlinCoroutinesVersion") {
                entry("kotlinx-coroutines-core")
                entry("kotlinx-coroutines-core-jvm")
                entry("kotlinx-coroutines-test")
            }

            dependencySet("org.springframework:$springVersion") {
                entry("spring-web")
                entry("spring-context")
            }

            dependency("org.springframework:spring-integration:$springIntegrationVersion")

            dependencySet("org.springframework.boot:$springBootVersion") {
                entry("spring-boot-starter-integration")
                entry("spring-boot-test")
            }

            dependency("org.springframework.integration:spring-integration-file:$springIntegrationVersion")

            dependency("org.slf4j:slf4j-api:1.7.30")
            dependency("io.github.microutils:kotlin-logging:2.1.21")
            dependency("ch.qos.logback:logback-classic:1.2.3")

            dependency("com.github.ladutsko:spring-boot-starter-hocon:2.0.0")
            dependency("com.typesafe:config:1.4.2")

            // Google Drive client
            dependency("com.google.oauth-client:google-oauth-client-jetty:$gDriveVersion")
            dependency("com.google.apis:google-api-services-drive:v3-rev20181101-$gDriveVersion")
            dependency("com.google.api-client:google-api-client:$gDriveVersion")

            dependencySet("io.kotest:5.3.0") {
                entry("kotest-runner-junit5-jvm")
                entry("kotest-property-jvm")
            }

            dependency("io.kotest.extensions:kotest-extensions-spring:1.1.1")

            dependency("io.mockk:mockk:1.12.3")

            dependency("com.fasterxml.jackson.core:jackson-databind:2.9.8")

            dependency("org.awaitility:awaitility-kotlin:$awaitilityVersion")

            dependency("com.jakewharton.byteunits:byteunits:0.9.1")

            dependency("org.apache.commons:commons-lang3:3.12.0")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "12"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        systemProperties["uploader.run.environment"] = rootProject.findProperty("uploader.run.environment")
    }
}

// Order checks with less expensive ones running first to fail fast
tasks.getByPath(":application:check").mustRunAfter(":test-shared-resources:check")
tasks.getByPath(":test-integration:check").mustRunAfter(":application:check")
tasks.getByPath(":test-e2e:check").mustRunAfter(":test-integration:check")
tasks.getByPath(":check").mustRunAfter(":test-e2e:check")

tasks.withType<GitSemverReleaseTask> { dependsOn.add(tasks.getByPath(":application:dockerPushImage")) }

tasks.withType<GitSemverReleaseFullTask> { dependsOn.addAll(allCheckTasks()) }

tasks.getByPath(":application:dockerPushImage").mustRunAfter(
        ":test-e2e:testContentTearDown",
        allCheckTasks()
)

fun allCheckTasks(): List<String> = rootProject
        .allprojects
        .flatMap { project -> project.tasks.filter { it.name == "check" } }
        .map { testTask -> testTask.path }