rootProject.name = "uploader"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            val kotlinVersion = "1.3.61" // todo tie to the one in gradle.properties

            val springBootVersion = "2.2.0.RELEASE" // todo tie to the one in gradle.properties

            val pluginVersionsByIds = mapOf(
                    "org.springframework.boot" to springBootVersion,
                    "io.spring.dependency-management" to "1.0.8.RELEASE",
                    "com.dorongold.task-tree" to "1.3.1",
                    "com.github.ben-manes.versions" to "0.20.0",
                    "com.bmuschko.docker-spring-boot-application" to "5.2.0"
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
                    logger.warn("No version configured in plugin management for id $pluginId in namespace $pluginNamespace")
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
