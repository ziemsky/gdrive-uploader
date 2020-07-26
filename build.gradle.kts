import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleaseFullTask
import com.ziemsky.gradle.git_semver_release_plugin.GitSemverReleaseTask
import com.ziemsky.gradle.git_semver_release_plugin.ProjectVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Paths

buildscript {
    dependencies {
        classpath(files("/home/ziemsky/projects/notmine/github-release-gradle-plugin/build/libs/github-release-2.2.12.jar"))
    }
}

//apply(com.github.breadmoirai.githubreleaseplugin.GithubReleasePlugin)

plugins {
    java
    kotlin("jvm") apply false

    id("io.spring.dependency-management")
    id("com.dorongold.task-tree")
    id("com.github.ben-manes.versions")
    id("com.github.breadmoirai.github-release")

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

allprojects {
    configurations.all { resolutionStrategy.failOnVersionConflict() }
}

// defined in gradle.properties; done this way to support referencing from buildscript
val awaitilityVersion: String by rootProject
val kotlinVersion: String by rootProject
val springBootVersion: String by rootProject
val springVersion: String by rootProject

val gDriveVersion = "1.27.0"

subprojects {
    repositories {
        mavenCentral()
        jcenter()
    }

    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencyManagement {
        // Note: io.spring.dependency-management plugin doesn't seem to support excluding transitive dependencies
        // and a number of main dependencies include conflicting versions of transitive ones (as flagged by
        // resolutionStrategy.failOnVersionConflict()). Declaring first-level transitive dependency to apply exclusion
        // to it doesn't help either (e.g. io.github.microutils:kotlin-logging:1.6.22).
        // For this reason, offending transitive dependencies had to be excluded at the dependency level in individual
        // sub-projects rather than here.

        dependencies {
            dependencySet("org.jetbrains.kotlin:$kotlinVersion") { // todo tie to the one in settings file
                entry("kotlin-stdlib-jdk8")
                entry("kotlin-stdlib")
                entry("kotlin-reflect")
            }

            dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")

            dependencySet("org.springframework:$springVersion") {
                entry("spring-web")
                entry("spring-context")
                entry("spring-integration")
            }

            dependencySet("org.springframework.boot:$springBootVersion") {
                entry("spring-boot-starter-integration")
                entry("spring-boot-test")
            }

            dependency("org.springframework.integration:spring-integration-file:$springVersion")


            dependency("org.slf4j:slf4j-api:1.7.30")
            dependency("io.github.microutils:kotlin-logging:1.8.0.1")
            dependency("ch.qos.logback:logback-classic:1.2.3")

            dependency("com.github.ladutsko:spring-boot-starter-hocon:2.0.0")
            dependency("com.typesafe:config:1.4.0")

            // Google Drive client
            dependency("com.google.oauth-client:google-oauth-client-jetty:$gDriveVersion")
            dependency("com.google.apis:google-api-services-drive:v3-rev20181101-$gDriveVersion")
            dependency("com.google.api-client:google-api-client:$gDriveVersion")

            dependencySet("io.kotest:4.1.0") {
                entry("kotest-runner-junit5-jvm")
                entry("kotest-property-jvm")
                entry("kotest-extensions-spring")
                entry("kotest-runner-console-jvm")
            }

            dependency("io.mockk:mockk:1.9")

            dependency("com.fasterxml.jackson.core:jackson-databind:2.9.8")

            dependency("org.awaitility:awaitility-kotlin:$awaitilityVersion")

            // Spring Boot starters already add Hibernate Validator but in a slightly older version
            dependency("org.hibernate.validator:hibernate-validator:6.0.14.Final")

            dependency("com.jakewharton.byteunits:byteunits:0.9.1")

            dependency("org.apache.commons:commons-lang3:3.9")
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

githubRelease {
    owner { "ziemsky" }

    repo { "gdrive-uploader" }

    releaseName { (rootProject.version as ProjectVersion).value() }

    tagName { (rootProject.version as ProjectVersion).asGitTagName() }

    overwrite { true }

    releaseAssets.from(
            fileTree(rootProject.project(":application").buildDir.absolutePath + "/libs")
    )

    applyPropertyIfProvided("github.api.token", this::token)
}

// Order checks with less expensive ones running first to fail fast
tasks.getByPath(":application:check").mustRunAfter(":test-shared-resources:check")
tasks.getByPath(":test-integration:check").mustRunAfter(":application:check")
tasks.getByPath(":test-e2e:check").mustRunAfter(":test-integration:check")
tasks.getByPath(":check").mustRunAfter(":test-e2e:check")

tasks.withType<GitSemverReleaseTask> { dependsOn.add(tasks.getByPath(":application:dockerPushImage")) }

tasks.withType<GitSemverReleaseFullTask> {
    dependsOn.addAll(allCheckTasks())
    finalizedBy(tasks.withType<GithubReleaseTask>())
}

tasks.getByPath(":application:dockerPushImage").mustRunAfter(
        ":test-e2e:testContentTearDown",
        allCheckTasks()
)

fun allCheckTasks(): List<String> = rootProject
        .allprojects
        .flatMap { project -> project.tasks.filter { it.name == "check" } }
        .map { testTask -> testTask.path }

fun applyPropertyIfProvided(propertyName: String, action: (String) -> Unit) {
    (rootProject.findProperty(propertyName) as String?)?.let(action)
}