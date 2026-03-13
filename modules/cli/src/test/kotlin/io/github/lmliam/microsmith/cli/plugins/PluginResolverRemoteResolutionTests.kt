package io.github.lmliam.microsmith.cli.plugins

import io.github.lmliam.microsmith.cli.command.RunCommand
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeTypeOf
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginResolverRemoteResolutionTests :
    StringSpec({
        "resolves plugin coordinate from configured repository and writes lockfile" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:microsmith-emitter-ts:1.4.2"
                publishMavenArtifact(repositoryRoot, coordinate)
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = output,
                        plugins = setOf(coordinate),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )
                val result =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            repositoryPolicy = fileRepositoryAllowedPolicy(),
                        ),
                    )

                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                success.classpath.shouldHaveSize(1)
                success.classpath.first().exists() shouldBe true
                val lockfilePath = requireNotNull(success.lockfilePath)
                lockfilePath.exists() shouldBe true
                val lockContents = lockfilePath.readLines().joinToString("\n")
                lockContents.shouldContain("version=2")
                lockContents.shouldContain("remote|$coordinate|")
                lockContents.shouldContain("remote-artifact|${parseCoordinate(coordinate).relativeJarPath}|")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "resolves transitive dependencies for plugin coordinates" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-transitive")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val rootCoordinate = "com.acme:plugin-root:1.0.0"
                val transitiveCoordinate = "com.acme:plugin-shared:2.1.0"
                publishMavenArtifact(repositoryRoot, transitiveCoordinate)
                publishMavenArtifact(repositoryRoot, rootCoordinate, dependencies = listOf(transitiveCoordinate))
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf(rootCoordinate),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            repositoryPolicy = fileRepositoryAllowedPolicy(),
                        ),
                    )

                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                success.classpath.shouldHaveSize(2)
                success.classpath.map { path -> path.fileName.toString() }.toSet() shouldBe
                    setOf("plugin-root-1.0.0.jar", "plugin-shared-2.1.0.jar")
                val lockfilePath = requireNotNull(success.lockfilePath)
                val lockContents = lockfilePath.readLines().joinToString("\n")
                lockContents.shouldContain("remote|$rootCoordinate|")
                lockContents.shouldContain("remote-artifact|${parseCoordinate(rootCoordinate).relativeJarPath}|")
                lockContents.shouldContain("remote-artifact|${parseCoordinate(transitiveCoordinate).relativeJarPath}|")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "excludes test and provided transitive dependencies from runtime classpath" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-runtime-scope")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val rootCoordinate = "com.acme:plugin-root:1.0.0"
                val runtimeCoordinate = "com.acme:runtime-dep:2.0.0"
                val testCoordinate = "com.acme:test-dep:2.0.0"
                val providedCoordinate = "com.acme:provided-dep:2.0.0"

                publishMavenArtifact(repositoryRoot, runtimeCoordinate)
                publishMavenArtifact(repositoryRoot, testCoordinate)
                publishMavenArtifact(repositoryRoot, providedCoordinate)
                publishMavenArtifact(
                    repositoryRoot = repositoryRoot,
                    coordinate = rootCoordinate,
                    dependencies = listOf(runtimeCoordinate, testCoordinate, providedCoordinate),
                    dependencyScopes =
                    mapOf(
                        testCoordinate to "test",
                        providedCoordinate to "provided",
                    ),
                )
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf(rootCoordinate),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            repositoryPolicy = fileRepositoryAllowedPolicy(),
                        ),
                    )

                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                val classpathNames = success.classpath.map { path -> path.fileName.toString() }.toSet()
                classpathNames shouldBe
                    setOf(
                        "plugin-root-1.0.0.jar",
                        "runtime-dep-2.0.0.jar",
                    )
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "deduplicates shared transitive dependencies deterministically" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-deterministic")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinateA = "com.acme:plugin-a:1.0.0"
                val coordinateB = "com.acme:plugin-b:1.0.0"
                val shared = "com.acme:plugin-shared:1.1.0"

                publishMavenArtifact(repositoryRoot, shared)
                publishMavenArtifact(repositoryRoot, coordinateA, dependencies = listOf(shared))
                publishMavenArtifact(repositoryRoot, coordinateB, dependencies = listOf(shared))
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf(coordinateA, coordinateB),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )

                val settings =
                    PluginResolverSettings(
                        cacheDirectory = cache,
                        repositoryPolicy = fileRepositoryAllowedPolicy(),
                    )

                val first =
                    resolvePlugins(command = command, settings = settings)
                        .shouldBeTypeOf<PluginResolutionResult.Success>()
                val second =
                    resolvePlugins(command = command, settings = settings)
                        .shouldBeTypeOf<PluginResolutionResult.Success>()

                val firstNames = first.classpath.map { path -> path.fileName.toString() }
                val secondNames = second.classpath.map { path -> path.fileName.toString() }
                firstNames shouldContainExactly secondNames
                firstNames.toSet() shouldBe
                    setOf(
                        "plugin-a-1.0.0.jar",
                        "plugin-b-1.0.0.jar",
                        "plugin-shared-1.1.0.jar",
                    )
                firstNames.size shouldBe 3
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails with remediation message when offline mode is used without lockfile metadata" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-offline")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:missing:1.0.0"),
                        offline = true,
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = tempDir.resolve("cache")),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("[offline-cache-miss]")
                failure.diagnostics.joinToString("\n").shouldContain("Offline mode requires a plugin lockfile")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails with categorized diagnostics when transitive dependency cannot be resolved" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-transitive-failure")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:plugin-root:1.0.0"
                val missing = "com.acme:missing-transitive:9.9.9"
                publishMavenArtifact(repositoryRoot, coordinate, dependencies = listOf(missing))
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf(coordinate),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            repositoryPolicy = fileRepositoryAllowedPolicy(),
                        ),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("[dependency-resolution]")
                message.shouldContain("Could not resolve plugin 'com.acme:plugin-root:1.0.0'")
                message.shouldContain("Verify plugin coordinates and repository availability")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "categorizes repository authentication failures and redacts sensitive values" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-auth-redaction")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val secretToken = "ghp_super_secret_token_12345"
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = output,
                        plugins = setOf("com.acme:secured-plugin:1.0.0"),
                    )

                val settings =
                    PluginResolverSettings(
                        cacheDirectory = tempDir.resolve("cache"),
                        repositoryCredentialsResolver =
                        object : RepositoryCredentialsResolver {
                            override fun resolve(repositoryUri: String): RepositoryCredentials = RepositoryCredentials(
                                username = "ci-user",
                                password = secretToken,
                            )

                            override fun sensitiveValues(): Set<String> = setOf(secretToken)
                        },
                        remotePluginResolver =
                        object : RemotePluginResolver {
                            override fun resolve(
                                coordinate: Coordinate,
                                repositories: List<RepositoryEndpoint>,
                                cacheDirectory: Path,
                                offline: Boolean,
                            ): ResolvedRemotePlugin {
                                repositories.first().credentials?.password shouldBe secretToken
                                throw PluginResolutionDiagnosticException(
                                    category = PluginResolverErrorCategory.AUTHENTICATION,
                                    message = "Unauthorized while using token '$secretToken'.",
                                )
                            }
                        },
                    )

                val result = resolvePlugins(command = command, settings = settings)
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("[authentication]")
                message.shouldContain("<redacted>")
                message.shouldNotContain(secretToken)
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails when cached artifact checksum does not match existing lockfile" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-lock-mismatch")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:lock-test:1.0.0"
                publishMavenArtifact(repositoryRoot, coordinate, jarContents = "initial".toByteArray())
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = output,
                        plugins = setOf(coordinate),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )

                resolvePlugins(
                    command = command,
                    settings =
                    PluginResolverSettings(
                        cacheDirectory = cache,
                        repositoryPolicy = fileRepositoryAllowedPolicy(),
                    ),
                )

                val cachedArtifact = cachePathFor(pluginArtifactCacheRoot(cache), parseCoordinate(coordinate))
                cachedArtifact.writeBytes("tampered".toByteArray())

                val mismatch =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            repositoryPolicy = fileRepositoryAllowedPolicy(),
                        ),
                    ).shouldBeTypeOf<PluginResolutionResult.Failure>()

                mismatch.diagnostics.joinToString("\n").shouldContain("Checksum mismatch")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "falls back to next repository when first http repository is unavailable" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-http-fallback")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:fallback-test:1.0.0"
                publishMavenArtifact(repositoryRoot, coordinate)
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = output,
                        plugins = setOf(coordinate),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            defaultRepositories =
                            listOf(
                                "http://127.0.0.1:1/repository",
                                repositoryRoot.toUri().toString(),
                            ),
                            repositoryPolicy =
                            fileRepositoryAllowedPolicy(
                                "http://127.0.0.1:1/repository",
                            ),
                        ),
                    )

                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                success.classpath.shouldHaveSize(1)
                success.classpath.first().exists() shouldBe true
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
