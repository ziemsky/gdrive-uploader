rootProject.name = "uploader"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            val kotlinVersion: String by settings

            val springBootVersion: String by settings

            val pluginVersionsByIds = mapOf(
                    "org.springframework.boot" to springBootVersion,
                    "io.spring.dependency-management" to "1.0.11.RELEASE",
                    "com.dorongold.task-tree" to "2.1.0",
                    "com.github.ben-manes.versions" to "0.42.0",
                    "com.bmuschko.docker-spring-boot-application" to "7.3.0",
                    "com.github.breadmoirai.github-release" to "2.3.7",
                    "com.palantir.git-version" to "0.12.3"
            )

            val pluginVersionsByNamespaces = mapOf(
                    "org.jetbrains.kotlin" to kotlinVersion,
                    "org.jetbrains.kotlin.plugin" to kotlinVersion
            )

            val pluginNamespace: String = requested.id.namespace ?: ""
            val pluginId: String = requested.id.toString()

            if (pluginVersionsByIds.containsKey(pluginId)) {
                useVersion(pluginVersionsByIds[pluginId])

            } else if (pluginVersionsByNamespaces.containsKey(pluginNamespace)) {
                useVersion(pluginVersionsByNamespaces[pluginNamespace])

            } else {
                if ("org.gradle" == pluginNamespace) {
                    // Versions of plugins from namespace 'org.gradle' are implicitly configured by current version of
                    // Gradle used.
                } else {
                    logger.warn("No version has been configured in plugin management for id $pluginId in namespace $pluginNamespace")
                }
            }
        }
    }
}

// todo optimise test execution order - make integration tests run before e2e to fail faster
include(
        "test-integration",
        "test-e2e",
        "test-shared-resources",
        "application"
)

includeBuild("git-semver-release-plugin")