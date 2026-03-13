pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://maven.pkg.github.com/lmliam/microsmith") {
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "me.liam.microsmith.gradle") {
                val version = providers.gradleProperty("microsmithVersion").orNull ?: error(
                    "Set microsmithVersion in gradle.properties or pass -PmicrosmithVersion=<version>.",
                )
                useModule("me.liam.microsmith:gradle-plugin:$version")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://maven.pkg.github.com/lmliam/microsmith") {
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}

rootProject.name = "java-gradle-native-fixture"
