import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") apply false

    id("io.spring.dependency-management")
    id("com.dorongold.task-tree")
    id("com.github.ben-manes.versions")
}

// todo document uploader.run.environment in readme
if (!project.hasProperty("uploader.run.environment")) {
    project.extra.set("uploader.run.environment", "local")
}

allprojects {
    configurations.all { resolutionStrategy.failOnVersionConflict() }
}

// defined in gradle.properties; done this way to support referencing from buildscript
val awaitilityVersion: String by rootProject
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
            dependencySet("org.jetbrains.kotlin:1.3.21") { // todo tie to the one in settings file
                entry("kotlin-stdlib-jdk8")
                entry("kotlin-stdlib")
                entry("kotlin-reflect")
            }

            dependencySet("org.springframework:5.1.3.RELEASE") {
                entry("spring-web")
                entry("spring-test")
                entry("spring-context")
                entry("spring-integration")
            }

            dependency("io.github.microutils:kotlin-logging:1.6.23")

            dependency("com.github.ladutsko:spring-boot-starter-hocon:2.0.0")
            dependency("com.typesafe:config:1.3.3")

            // Google Drive client
            dependency("com.google.oauth-client:google-oauth-client-jetty:$gDriveVersion")
            dependency("com.google.apis:google-api-services-drive:v3-rev20181101-$gDriveVersion")
            dependency("com.google.api-client:google-api-client:$gDriveVersion")

            dependencySet("io.kotlintest:3.2.1") {
                entry("kotlintest-runner-junit5")
                entry("kotlintest-extensions-spring")
            }

            dependency("io.mockk:mockk:1.9")

            dependency("com.fasterxml.jackson.core:jackson-databind:2.9.8")

            dependency("org.awaitility:awaitility-kotlin:$awaitilityVersion")

            // Spring Boot starters already add Hibernate Validator but in a slightly older version
            dependency("org.hibernate.validator:hibernate-validator:6.0.14.Final")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        systemProperties["uploader.run.environment"] = rootProject.findProperty("uploader.run.environment")
    }
}


tasks.getByPath(":application:check").mustRunAfter(":test-shared-resources:check")
tasks.getByPath(":test-integration:check").mustRunAfter(":application:check")
tasks.getByPath(":test-e2e:check").mustRunAfter(":test-integration:check")
tasks.getByPath(":check").mustRunAfter(":test-e2e:check")
