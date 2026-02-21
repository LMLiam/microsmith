package me.liam.microsmith.cli.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginResolverTests :
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
                lockfilePath.readLines().joinToString("\n").shouldContain("version=1")
                lockfilePath.readLines().joinToString("\n").shouldContain("remote|$coordinate|")
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

        "fails with remediation message when offline plugin artifact is missing from cache" {
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
                failure.diagnostics.joinToString("\n").shouldContain("Offline mode is enabled")
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
                    )
                        .shouldBeTypeOf<PluginResolutionResult.Failure>()

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

        "uses relative plugin jar lock key instead of absolute machine path" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-local-key")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val localJar = tempDir.resolve("plugins/local-plugin.jar")
                localJar.parent?.toFile()?.mkdirs()
                localJar.writeBytes("local-plugin".toByteArray())
                script.writeText("// test script")

                val workingDirectory = Path.of("").toAbsolutePath().normalize()
                val relativeJarPath = workingDirectory.relativize(localJar.toAbsolutePath().normalize())
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
                success.classpath.shouldHaveSize(1)
                val lockfilePath = requireNotNull(success.lockfilePath)
                val lockContents = lockfilePath.readLines().joinToString("\n")
                val expectedKey = relativeJarPath.normalize().toString().replace('\\', '/')
                lockContents.shouldContain("local|$expectedKey|")
                lockContents.contains("local|${localJar.toAbsolutePath().normalize()}|") shouldBe false
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
                failure.diagnostics.joinToString("\n").shouldContain("file:// repositories are not allowed")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails when plugin checksum allowlist is missing requested plugin entry" {
            val tempDir = createTempDirectory("microsmith-plugin-allowlist-missing-entry")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val localJar = tempDir.resolve("plugins/custom.jar")
                localJar.parent?.toFile()?.mkdirs()
                localJar.writeBytes("allowlist-check".toByteArray())

                val workingDirectory = Path.of("").toAbsolutePath().normalize()
                val relativeJarPath = workingDirectory.relativize(localJar.toAbsolutePath().normalize())
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
            val tempDir = createTempDirectory("microsmith-plugin-allowlist-mismatch")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val localJar = tempDir.resolve("plugins/custom.jar")
                localJar.parent?.toFile()?.mkdirs()
                localJar.writeBytes("allowlist-check".toByteArray())

                val workingDirectory = Path.of("").toAbsolutePath().normalize()
                val relativeJarPath = workingDirectory.relativize(localJar.toAbsolutePath().normalize())
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

private fun fileRepositoryAllowedPolicy(vararg additionalAllowedRepositories: String): RepositoryAllowlistPolicy =
    RepositoryAllowlistPolicy(
        allowedRepositories =
        (listOf(MAVEN_CENTRAL_REPOSITORY) + additionalAllowedRepositories)
            .map(::normalizeRepositoryUri)
            .toSet(),
        allowFileRepositories = true,
    )

private fun publishMavenArtifact(
    repositoryRoot: Path,
    coordinate: String,
    dependencies: List<String> = emptyList(),
    jarContents: ByteArray = "plugin-jar-contents".toByteArray(),
) {
    val parsed = parseCoordinate(coordinate)
    val base =
        repositoryRoot
            .resolve(parsed.group.replace('.', '/'))
            .resolve(parsed.artifact)
            .resolve(parsed.version)
    base.createDirectories()

    val pomPath = base.resolve("${parsed.artifact}-${parsed.version}.pom")
    val dependenciesBlock =
        if (dependencies.isEmpty()) {
            ""
        } else {
            dependencies.joinToString(
                separator = "\n",
                prefix = "<dependencies>\n",
                postfix = "\n</dependencies>",
            ) { dependency ->
                val dep = parseCoordinate(dependency)
                """
                <dependency>
                  <groupId>${dep.group}</groupId>
                  <artifactId>${dep.artifact}</artifactId>
                  <version>${dep.version}</version>
                </dependency>
                """.trimIndent()
            }
        }
    val pomXml =
        """
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>
          <groupId>${parsed.group}</groupId>
          <artifactId>${parsed.artifact}</artifactId>
          <version>${parsed.version}</version>
          <packaging>jar</packaging>
          $dependenciesBlock
        </project>
        """.trimIndent()
    pomPath.writeText(pomXml)

    val jarPath = base.resolve("${parsed.artifact}-${parsed.version}.jar")
    jarPath.writeBytes(jarContents)
}
