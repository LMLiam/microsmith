package io.github.lmliam.microsmith.cli.plugins

import io.github.lmliam.microsmith.cli.command.RunCommand
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeTypeOf
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readLines
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginResolverValidationTests :
    StringSpec({
        "uses relative plugin jar lock key instead of absolute machine path" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-resolver-local-key")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val localJar = tempDir.resolve("plugins/local-plugin.jar")
                localJar.parent.createDirectories()
                localJar.writeBytes("local-plugin".toByteArray())
                script.writeText("// test script")

                val relativeJarPath = relativizeFromWorkingDirectory(localJar)
                val command =
                    RunCommand(
                        script = script,
                        outputDir = output,
                        pluginJars = setOf(relativeJarPath),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = cache),
                    )

                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                val lockfilePath = requireNotNull(success.lockfilePath)
                val lockContents = lockfilePath.readLines().joinToString("\n")
                val expectedKey = relativeJarPath.normalize().toString().replace('\\', '/')
                lockContents.shouldContain("local|$expectedKey|")
                lockContents.contains("local|${localJar.toAbsolutePath().normalize()}|") shouldBe false
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "reports credential lookup failures as authentication diagnostics" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-credential-lookup")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:plugin:1.0.0"),
                        repositoryOverride = "https://packages.acme.internal/maven",
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = tempDir.resolve("cache"),
                        defaultRepositories = emptyList(),
                        repositoryPolicy =
                        RepositoryAllowlistPolicy(
                            allowedRepositories = setOf(
                                normalizeRepositoryUri("https://packages.acme.internal/maven"),
                            ),
                        ),
                        repositoryCredentialsResolver =
                        object : RepositoryCredentialsResolver {
                            override fun resolve(repositoryUri: String): RepositoryCredentials =
                                throw IllegalArgumentException("Repository credentials are invalid.")
                        },
                        remotePluginResolver =
                        object : RemotePluginResolver {
                            override fun resolve(
                                coordinate: Coordinate,
                                repositories: List<RepositoryEndpoint>,
                                cacheDirectory: Path,
                                offline: Boolean,
                            ): ResolvedRemotePlugin {
                                error("Remote resolution should not start when credential lookup fails.")
                            }
                        },
                    )

                val result = resolvePlugins(command = command, settings = settings)
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("[authentication]")
                message.shouldNotContain("[repository-policy]")
                message.shouldContain("Repository credentials are invalid.")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "rejects plugin coordinates with traversal path segments" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-path-traversal")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:..:1.0.0"),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = tempDir.resolve("cache")),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("invalid path segment")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "rejects plugin coordinates containing lockfile delimiter" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-lock-delimiter")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:bad|artifact:1.0.0"),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = tempDir.resolve("cache")),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("reserved lockfile delimiter")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "rejects plugin coordinates containing path separators" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-path-separator")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:bad/path:1.0.0"),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = tempDir.resolve("cache")),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("path separator")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "rejects lockfile delimiter in group segment" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-group-delimiter")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com|acme:plugin:1.0.0"),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = tempDir.resolve("cache")),
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("reserved lockfile delimiter")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "blocks unapproved repository endpoint by default allowlist policy" {
            val tempDir = createTempDirectory("microsmith-plugin-repository-allowlist")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:plugin:1.0.0"),
                        repositoryOverride = "https://packages.acme.internal/maven",
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = tempDir),
                    )
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("[repository-policy]")
                failure.diagnostics.joinToString("\n").shouldContain("allowed repository allowlist")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "blocks file repository endpoint by default security policy" {
            val tempDir = createTempDirectory("microsmith-plugin-repository-file-blocked")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:file-blocked:1.0.0"
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
                        settings = PluginResolverSettings(cacheDirectory = cache),
                    )
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("[repository-policy]")
                failure.diagnostics.joinToString("\n").shouldContain("file:// repositories are not allowed")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails when plugin checksum allowlist is missing requested plugin entry" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-allowlist-missing-entry")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val localJar = tempDir.resolve("plugins/custom.jar")
                localJar.parent.createDirectories()
                localJar.writeBytes("allowlist-check".toByteArray())

                val relativeJarPath = relativizeFromWorkingDirectory(localJar)
                val lockKey = relativeJarPath.normalize().toString().replace('\\', '/')

                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        pluginJars = setOf(relativeJarPath),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = tempDir.resolve("cache"),
                            checksumAllowlist = PluginChecksumAllowlist(entries = emptyMap()),
                        ),
                    )
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("missing required entries")
                failure.diagnostics.joinToString("\n").shouldContain(lockKey)
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails when plugin checksum allowlist hash does not match artifact checksum" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-allowlist-mismatch")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val localJar = tempDir.resolve("plugins/custom.jar")
                localJar.parent.createDirectories()
                localJar.writeBytes("allowlist-check".toByteArray())

                val relativeJarPath = relativizeFromWorkingDirectory(localJar)
                val lockKey = relativeJarPath.normalize().toString().replace('\\', '/')
                val invalidChecksum = "0".repeat(64)

                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        pluginJars = setOf(relativeJarPath),
                    )

                val allowlist =
                    PluginChecksumAllowlist(
                        entries =
                        mapOf(
                            LockKey(kind = LOCAL_KIND, key = lockKey) to invalidChecksum,
                        ),
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings =
                        PluginResolverSettings(
                            cacheDirectory = tempDir.resolve("cache"),
                            checksumAllowlist = allowlist,
                        ),
                    )
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("Allowlist checksum mismatch")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
