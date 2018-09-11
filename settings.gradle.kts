rootProject.name = "uploader"

include(
        "test-e2e",
        "test-integration",
        "test-shared-resources",
        "application"
)

//pluginManagement {
//    repositories {
//        mavenCentral()
//    }
//
//    resolutionStrategy {
//        eachPlugin {
//            if (requested.id.namespace == "org.jetbrains.kotlin") {
//                useVersion("1.2.70")
//            }
//        }
//    }
//}