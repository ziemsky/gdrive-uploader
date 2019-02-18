rootProject.name = "uploader"

val kotlinVersion = "1.3.21" // todo tie to the one in main build script

val springBootVersion = "2.1.3.RELEASE" // todo tie to the one in gradle.properties

val pluginVersionsByIds = mapOf(
        "org.springframework.boot" to springBootVersion,
        "io.spring.dependency-management" to "1.0.6.RELEASE",
        "com.github.johnrengelman.processes" to "0.5.0",
        "com.dorongold.task-tree" to "1.3.1",
        "com.github.ben-manes.versions" to "0.20.0"
)

val pluginVersionsByNamespaces = mapOf(
        "org.jetbrains.kotlin" to kotlinVersion,
        "org.jetbrains.kotlin.plugin" to kotlinVersion
)

// todo try reducing both maps to one and rely on String.startsWith?

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
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
