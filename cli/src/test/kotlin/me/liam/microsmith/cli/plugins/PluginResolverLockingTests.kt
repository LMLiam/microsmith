package me.liam.microsmith.cli.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import me.liam.microsmith.cli.command.RunCommand
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readLines
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginResolverLockingTests :
    StringSpec({
        "migrates existing v1 lockfile to graph-aware v2 format" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-lockfile-migration")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val rootCoordinate = "com.acme:plugin-root:1.0.0"
                val transitiveCoordinate = "com.acme:plugin-shared:2.1.0"
                publishMavenArtifact(repositoryRoot, transitiveCoordinate)
                publishMavenArtifact(repositoryRoot, rootCoordinate, dependencies = listOf(transitiveCoordinate))
                script.writeText("// test script")

                val v1LockfilePath = defaultLockfilePath(script)
                val rootJar = repositoryRoot.resolve(parseCoordinate(rootCoordinate).relativeJarPath)
                v1LockfilePath.writeText(
                    """
                    version=1
                    remote|$rootCoordinate|${sha256(rootJar)}
                    """.trimIndent(),
                )

                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf(rootCoordinate),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = cache,
                        repositoryPolicy = fileRepositoryAllowedPolicy(),
                    )

                val success =
                    resolvePlugins(command = command, settings = settings)
                        .shouldBeTypeOf<PluginResolutionResult.Success>()
                val lockfilePath = requireNotNull(success.lockfilePath)
                val lockContents = lockfilePath.readLines().joinToString("\n")
                lockContents.shouldContain("version=2")
                lockContents.shouldContain("remote|$rootCoordinate|")
                lockContents.shouldContain("remote-artifact|${parseCoordinate(rootCoordinate).relativeJarPath}|")
                lockContents.shouldContain("remote-artifact|${parseCoordinate(transitiveCoordinate).relativeJarPath}|")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails offline when lockfile is v1 and cannot validate full graph" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-offline-v1-lock")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:offline-lock:1.0.0"
                publishMavenArtifact(repositoryRoot, coordinate)
                script.writeText("// test script")

                val lockfilePath = defaultLockfilePath(script)
                val rootJar = repositoryRoot.resolve(parseCoordinate(coordinate).relativeJarPath)
                lockfilePath.writeText(
                    """
                    version=1
                    remote|$coordinate|${sha256(rootJar)}
                    """.trimIndent(),
                )

                val result =
                    resolvePlugins(
                        command =
                        RunCommand(
                            script = script,
                            outputDir = tempDir.resolve("generated"),
                            plugins = setOf(coordinate),
                            repositoryOverride = repositoryRoot.toUri().toString(),
                            offline = true,
                        ),
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            repositoryPolicy = fileRepositoryAllowedPolicy(),
                        ),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("[offline-cache-miss]")
                message.shouldContain("requires lockfile version 2")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails offline when locked transitive artifact is missing from cache" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-offline-graph-missing")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val rootCoordinate = "com.acme:plugin-root:1.0.0"
                val transitiveCoordinate = "com.acme:plugin-shared:2.1.0"
                publishMavenArtifact(repositoryRoot, transitiveCoordinate)
                publishMavenArtifact(repositoryRoot, rootCoordinate, dependencies = listOf(transitiveCoordinate))
                script.writeText("// test script")

                val baseCommand =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf(rootCoordinate),
                        repositoryOverride = repositoryRoot.toUri().toString(),
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = cache,
                        repositoryPolicy = fileRepositoryAllowedPolicy(),
                    )

                resolvePlugins(command = baseCommand, settings = settings)
                    .shouldBeTypeOf<PluginResolutionResult.Success>()

                val cachedTransitive =
                    cachePathFor(pluginArtifactCacheRoot(cache), parseCoordinate(transitiveCoordinate))
                cachedTransitive.toFile().delete()

                val failure =
                    resolvePlugins(command = baseCommand.copy(offline = true), settings = settings)
                        .shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("[offline-cache-miss]")
                message.shouldContain(parseCoordinate(transitiveCoordinate).relativeJarPath)
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails when transitive cached artifact checksum does not match existing lockfile" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-transitive-lock-mismatch")
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
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = cache,
                        repositoryPolicy = fileRepositoryAllowedPolicy(),
                    )

                resolvePlugins(command = command, settings = settings)
                    .shouldBeTypeOf<PluginResolutionResult.Success>()

                val cachedTransitive =
                    cachePathFor(pluginArtifactCacheRoot(cache), parseCoordinate(transitiveCoordinate))
                cachedTransitive.writeBytes("tampered-transitive".toByteArray())

                val failure =
                    resolvePlugins(command = command.copy(offline = true), settings = settings)
                        .shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("Checksum mismatch for remote-artifact plugin")
                message.shouldContain(parseCoordinate(transitiveCoordinate).relativeJarPath)
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails when plugin checksum allowlist omits transitive remote artifacts" {
            val tempDir = createTempDirectory("microsmith-plugin-allowlist-transitive-missing")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val rootCoordinate = "com.acme:plugin-root:1.0.0"
                val transitiveCoordinate = "com.acme:plugin-shared:2.1.0"
                publishMavenArtifact(repositoryRoot, transitiveCoordinate)
                publishMavenArtifact(repositoryRoot, rootCoordinate, dependencies = listOf(transitiveCoordinate))
                script.writeText("// test script")

                val rootJar = repositoryRoot.resolve(parseCoordinate(rootCoordinate).relativeJarPath)
                val allowlist =
                    PluginChecksumAllowlist(
                        entries =
                        mapOf(
                            LockKey(kind = REMOTE_KIND, key = rootCoordinate) to sha256(rootJar),
                        ),
                    )

                val result =
                    resolvePlugins(
                        command =
                        RunCommand(
                            script = script,
                            outputDir = tempDir.resolve("generated"),
                            plugins = setOf(rootCoordinate),
                            repositoryOverride = repositoryRoot.toUri().toString(),
                        ),
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = cache,
                            repositoryPolicy = fileRepositoryAllowedPolicy(),
                            checksumAllowlist = allowlist,
                        ),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("Plugin allowlist is missing required entries")
                message.shouldContain("remote-artifact:${parseCoordinate(transitiveCoordinate).relativeJarPath}")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
