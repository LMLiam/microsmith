# Native Gradle integration

Use the Gradle plugin as the primary path for Java and Kotlin repositories that already build with Gradle and want imported-project IDE support.

Prefer this path when:

- the repository already has a Gradle wrapper and existing Gradle build conventions
- generation should run as `./gradlew microsmithGenerate`
- JetBrains IDEs should resolve `.microsmith.kts` symbols from the imported Gradle project instead of a separate helper project

Canonical contract:

1. Configure plugin resolution in `settings.gradle.kts`.
2. Configure normal dependency repositories as well, because the plugin resolves `runtime-scripting` and any `microsmithPlugins` entries as standard Gradle dependencies.
3. Apply plugin id `io.github.lmliam.microsmith`.
4. Configure `microsmith { ... }`.
5. Run `./gradlew microsmithGenerate`.

Minimal settings example:

```kotlin
pluginManagement {
    val microsmithVersion = providers.gradleProperty("microsmithVersion").orNull ?: error(
        "Set microsmithVersion in gradle.properties or pass -PmicrosmithVersion=<version>.",
    )

    repositories {
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
    plugins {
        id("io.github.lmliam.microsmith") version microsmithVersion
    }
}

dependencyResolutionManagement {
    repositories {
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
```

Minimal build example:

```kotlin
plugins {
    java
    id("io.github.lmliam.microsmith")
}

microsmith {
    scriptFile.set(layout.projectDirectory.file("build.microsmith.kts"))
}

tasks.named("check") {
    dependsOn(tasks.named("microsmithGenerate"))
}
```

The native Gradle plugin exposes:

- `microsmithGenerate`: runs the configured `.microsmith.kts` script in a dedicated worker JVM so Gradle's embedded Kotlin runtime does not collide with Microsmith's scripting host
- `microsmith`: extension for `scriptFile`, `outputDirectory`, `variables`, and `flags`
- `microsmithPlugins`: resolvable configuration for external Microsmith provider plugins
- `microsmithIde`: IDE-facing classpath that includes Microsmith runtime types and plugin-provided types and is wired into `compileOnly` for Java-base projects

External plugin example:

```kotlin
dependencies {
    add("microsmithPlugins", "com.acme:microsmith-emitter-ts:1.4.2")
}
```

Generated-source guidance:

- Microsmith does not auto-register generated directories as Java or Kotlin sources
- Microsmith writes generated `.proto` files under `./proto` by default
- only add generated directories into source sets when a generator actually emits compilable language sources for that repository
- use the same explicit Gradle source-set wiring you already use for other generated sources; wire `./proto` only when you intentionally consume generated protobuf sources from the build

Coexistence with the CLI, helper, and fallback paths:

- `microsmith init` still works in Gradle repositories and is the fastest way to scaffold `build.microsmith.kts`
- once native Gradle integration is adopted, use `./gradlew microsmithGenerate` as the primary generation path
- the JetBrains IDE helper and fallback jar remain useful when you intentionally keep Microsmith outside the Gradle build, or when you are validating CLI-managed flows
- representative native Gradle fixtures live in `examples/gradle/java`, `examples/gradle/kotlin`, and `examples/gradle/dotnet`

[Back to the README](../README.md)
