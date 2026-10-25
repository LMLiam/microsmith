# Releases

## Example fixtures

The repository includes fixture repositories under `examples/non-gradle/`, `examples/jvm/`, `examples/gradle/`, and `examples/maven/`.
CLI-managed fixtures exercise `microsmith init` and direct `microsmith run` flows. Native Gradle fixtures exercise `./gradlew microsmithGenerate`. Native Maven fixtures exercise `mvn microsmith:generate`.

| Fixture                | Directory                    | Local command from fixture root                                                |
|------------------------|------------------------------|--------------------------------------------------------------------------------|
| Java (native Gradle)   | `examples/gradle/java`       | `./gradlew microsmithGenerate -PmicrosmithVersion=<version>`                   |
| Kotlin (native Gradle) | `examples/gradle/kotlin`     | `./gradlew microsmithGenerate -PmicrosmithVersion=<version>`                   |
| .NET (native Gradle)   | `examples/gradle/dotnet`     | `./gradlew microsmithGenerate -PmicrosmithVersion=<version>`                   |
| Java (native Maven)    | `examples/maven/java`        | `mvn microsmith:generate -Dmicrosmith.version=<version>`                       |
| Kotlin (native Maven)  | `examples/maven/kotlin`      | `mvn microsmith:generate -Dmicrosmith.version=<version>`                       |
| .NET (native Maven)    | `examples/maven/dotnet`      | `mvn microsmith:generate -Dmicrosmith.version=<version>`                       |
| Java (CLI)             | `examples/jvm/java-maven`    | `microsmith init` then `microsmith run build.microsmith.kts`                   |
| Kotlin (CLI)           | `examples/gradle/kotlin`     | `microsmith init` then `microsmith run build.microsmith.kts`                   |
| Node                   | `examples/non-gradle/node`   | `microsmith init` then `microsmith run build.microsmith.kts`                   |
| Go                     | `examples/non-gradle/go`     | `microsmith init` then `microsmith run build.microsmith.kts`                   |
| Python                 | `examples/non-gradle/python` | `microsmith init` then `microsmith run build.microsmith.kts`                   |
| Ruby                   | `examples/non-gradle/ruby`   | `microsmith init` then `microsmith run build.microsmith.kts`                   |
| Rust                   | `examples/non-gradle/rust`   | `microsmith init` then `microsmith run build.microsmith.kts`                   |
| .NET                   | `examples/non-gradle/dotnet` | `microsmith init` then `microsmith run build.microsmith.kts --out .\Generated` |

## Validation matrix and release checklist

### Automated validation matrix

| Surface                       | Coverage source                                | Evidence                                                                                                                                          |
|-------------------------------|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| CLI distribution verification        | `cli-verification` on Ubuntu, macOS, and Windows      | Dist launcher, generation, process isolation, and `doctor --diagnostics json`.                                                                    |
| Installer and bootstrap verification | `cli-verification` on Ubuntu, macOS, and Windows      | Installer, `--version`, `microsmith init`, and canonical `init -> run` generation.                                                                |
| Native Gradle fixture verification   | `build-and-qodana` on Ubuntu                   | Publishes required packages to `mavenLocal`, then runs `microsmithGenerate` for Java, Kotlin, and .NET Gradle fixtures.                            |
| Native Maven fixture verification    | `build-and-qodana` on Ubuntu                   | Publishes required packages to `mavenLocal`, then runs `mvn microsmith:generate` for Java, Kotlin, and .NET Maven fixtures.                        |
| Consumer fixture onboarding   | `cli-verification` on Ubuntu, macOS, and Windows      | Ubuntu covers Java, Kotlin, Node, Go, Python, Ruby, Rust, and .NET; macOS and Windows cover the .NET fixture through dist or installer flows.       |
| JetBrains helper lifecycle    | `cli-verification` on Ubuntu, macOS, and Windows      | Ubuntu covers Java, Kotlin, Node, Go, Python, Ruby, Rust, and .NET helper flows; macOS and Windows validate the .NET helper path.                  |
| Fallback artifact packaging   | `build-and-qodana` on Ubuntu                   | `:runtime-scripting:ideFallbackArtifacts` and `:runtime-scripting:generateIdeFallbackChecksums`.                                                  |
| Resolver, auth, lock, offline | `./gradlew build` and module regression suites | `:cli:jvmKotest` covers authenticated repositories, lockfiles, cache policy, and offline behavior.                                                |

Automated gates are required on every release candidate commit:

- `Build & Qodana`
- `cli-verification` on `ubuntu-latest`
- `cli-verification` on `macos-latest`
- `cli-verification` on `windows-latest`

### JetBrains IDE validation bands

JetBrains IDE indexing and import behavior is partly host-driven, so final sign-off stays manual even though CI covers helper generation, helper doctor, and fallback artifact packaging.

Support policy:

- supported products are IntelliJ IDEA, WebStorm, GoLand, Rider, PyCharm, RubyMine, and RustRover
- supported release band is the current stable major line and the previous stable major line at release cut
- EAP builds are best-effort only and do not block release

### Native Gradle IDE validation

| Product                      | Fixture root             | Required validation path                                                                                                                                             |
|------------------------------|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| IntelliJ IDEA                | `examples/gradle/java`   | Import the fixture root as a Gradle project, refresh Gradle indexing, and confirm `build.microsmith.kts` resolves Microsmith DSL symbols without the helper project. |
| IntelliJ IDEA                | `examples/gradle/kotlin` | Import the fixture root as a Gradle project, refresh Gradle indexing, and confirm `build.microsmith.kts` resolves Microsmith DSL symbols without the helper project. |

### Native Maven IDE validation

| Product                      | Fixture root            | Required validation path                                                                                                                                                       |
|------------------------------|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| IntelliJ IDEA                | `examples/maven/java`   | Import the fixture root as a Maven project, reload Maven indexing, and confirm `build.microsmith.kts` resolves the built-in Microsmith DSL symbols without the helper project. |
| IntelliJ IDEA                | `examples/maven/kotlin` | Import the fixture root as a Maven project, reload Maven indexing, and confirm `build.microsmith.kts` resolves the built-in Microsmith DSL symbols without the helper project. |

Native Gradle checklist:

1. Install or publish the release-candidate Microsmith Gradle plugin and runtime packages.
2. Import the fixture root as a Gradle project in the target JetBrains IDE.
3. Refresh Gradle indexing.
4. Run `./gradlew microsmithGenerate`.
5. Confirm `microsmith {}`, `schemas {}`, and `protobuf {}` resolve in `build.microsmith.kts` without importing `.microsmith/ide`.

Native Maven checklist:

1. Install or publish the release-candidate Microsmith Maven plugin and runtime packages.
2. Import the fixture root as a Maven project in IntelliJ IDEA.
3. Reload Maven indexing.
4. Run `mvn microsmith:generate -Dmicrosmith.version=<version>`.
5. Confirm `microsmith {}`, `schemas {}`, and `protobuf {}` resolve in `build.microsmith.kts` without importing `.microsmith/ide`.

### Helper and fallback IDE validation

| Product       | Fixture root                 | Required helper path                                 | Required fallback path                                 |
|---------------|------------------------------|------------------------------------------------------------|--------------------------------------------------------------|
| WebStorm      | `examples/non-gradle/node`   | Run the helper checklist below against the Node fixture.   | Run the fallback checklist below against the Node fixture.   |
| IntelliJ IDEA | `examples/jvm/java-maven`    | Run the helper checklist below against the Java fixture.   | Run the fallback checklist below against the Java fixture.   |
| IntelliJ IDEA | `examples/gradle/kotlin`     | Run the helper checklist below against the Kotlin fixture. | Run the fallback checklist below against the Kotlin fixture. |
| GoLand        | `examples/non-gradle/go`     | Run the helper checklist below against the Go fixture.     | Run the fallback checklist below against the Go fixture.     |
| PyCharm       | `examples/non-gradle/python` | Run the helper checklist below against the Python fixture. | Run the fallback checklist below against the Python fixture. |
| RubyMine      | `examples/non-gradle/ruby`   | Run the helper checklist below against the Ruby fixture.   | Run the fallback checklist below against the Ruby fixture.   |
| RustRover     | `examples/non-gradle/rust`   | Run the helper checklist below against the Rust fixture.   | Run the fallback checklist below against the Rust fixture.   |
| Rider         | `examples/non-gradle/dotnet` | Run the helper checklist below against the .NET fixture.   | Run the fallback checklist below against the .NET fixture.   |

### Manual IDE release checklist

Helper path:

1. Install the release-candidate Microsmith CLI.
2. Run `microsmith init` from the fixture root.
3. Import `.microsmith/ide/build.gradle.kts` as a Gradle project in the target JetBrains IDE.
4. Refresh Gradle indexing.
5. Run `microsmith ide doctor --repo-root <fixture> --diagnostics json --verbose`.
6. Confirm `microsmith {}`, `schemas {}`, and `protobuf {}` resolve in `build.microsmith.kts` without red squiggles.

Fallback path:

1. Download or build the matching `microscript-definition-<version>-all.jar`.
2. Attach the jar as a project library in the target JetBrains IDE.
3. Reindex the project.
4. Confirm `microsmith {}`, `schemas {}`, and `protobuf {}` resolve in `build.microsmith.kts`.
5. Confirm plugin-provided types remain a helper-only capability unless the helper project is imported.

Release sign-off artifact:

- record one row per product and IDE version in the release PR or release issue
- include product, IDE version, fixture root, helper result, fallback result, verifier GitHub handle, and date
- treat that release record as the required manual sign-off artifact
- do not cut the release until all required rows for the supported version band are recorded and approved

## Distribution and release artifacts

### Release asset contents

`./gradlew :cli:releaseArtifacts :runtime-scripting:ideFallbackArtifacts :runtime-scripting:generateIdeFallbackChecksums` produces:

- `modules/integrations/cli/build/libs/microsmith-cli-<version>-all.jar`
- `modules/integrations/cli/build/distributions/microsmith-cli-<version>-dist.zip`
- `modules/integrations/cli/build/distributions/microsmith-cli-<version>-dist.tar.gz`
- `modules/integrations/cli/build/release-assets/microsmith-install.sh`
- `modules/integrations/cli/build/release-assets/microsmith-install.ps1`
- `modules/integrations/cli/build/release-assets/*.sha256`
- `modules/integrations/cli/build/generated/microsmith/bundled-plugins.lock`
- `modules/runtime/runtime-scripting/build/release-assets/microscript-definition-<version>-all.jar`
- `modules/runtime/runtime-scripting/build/release-assets/microscript-definition-<version>-all.jar.sha256`

Published packages are available from:

```text
https://maven.pkg.github.com/lmliam/microsmith
```

### Useful build tasks

- `:cli:generateBundledPluginCatalog`
- `:cli:shadowJar`
- `:cli:prepareDist`
- `:cli:cliDistZip`
- `:cli:cliDistTar`
- `:cli:verifyDistLayout`
- `:cli:distArtifacts`
- `:cli:generateReleaseChecksums`
- `:cli:releaseArtifacts`
- `:runtime-scripting:verifyIdeFallbackShadowJar`
- `:runtime-scripting:ideFallbackArtifacts`
- `:runtime-scripting:generateIdeFallbackChecksums`

### Packaging model

Microsmith ships with:

- installer scripts that provision Java 24 automatically when needed
- manual fat jar and unpacked distribution channels for teams that want explicit runtime management
- checksum sidecars for release verification
- no requirement for Gradle or Maven in consuming repositories

[Back to the README](../README.md)
