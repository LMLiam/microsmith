package me.liam.microsmith.cli.init

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.ide.IdeHelperConflictException
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class InitBootstrapTests :
    StringSpec({
        "creates repo-aware bootstrap files and invokes IDE helper refresh" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-create")
            repoRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")
            try {
                val helperRoot = repoRoot.resolve(".microsmith/ide")
                val result =
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot),
                        ideRefreshRunner = { command ->
                            IdeHelperRefreshResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                helperRoot = helperRoot,
                                updatedFiles = listOf(helperRoot.resolve("build.gradle.kts")),
                                classpathEntries = listOf(repoRoot.resolve("runtime/microsmith-cli-all.jar")),
                            )
                        },
                    )

                result.repositoryDetection.type shouldBe OnboardingRepositoryType.NODE
                result.repositoryDetection.matchedMarkers shouldBe listOf("package.json")
                result.createdFiles.shouldContainExactly(
                    listOf(
                        repoRoot.resolve("build.microsmith.kts"),
                        repoRoot.resolve("settings.microsmith.kts"),
                    ),
                )
                result.overwrittenFiles shouldBe emptyList()
                result.preservedFiles shouldBe emptyList()
                result.ideHelperResult?.helperRoot shouldBe helperRoot
                repoRoot.resolve("build.microsmith.kts").isRegularFile() shouldBe true
                repoRoot.resolve("settings.microsmith.kts").isRegularFile() shouldBe true
                repoRoot.resolve("build.microsmith.kts").readText().shouldContain("NodeUserCreated")
                repoRoot.resolve("build.microsmith.kts").readText().shouldContain("./generated")
                repoRoot.resolve("settings.microsmith.kts").readText().shouldContain("Detected repository type: Node")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "preserves existing bootstrap files on repeated init runs by default" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-idempotent")
            val existingBuild = repoRoot.resolve("build.microsmith.kts")
            existingBuild.writeText("// existing build script")
            try {
                val helperRoot = repoRoot.resolve(".microsmith/ide")
                val result =
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot),
                        ideRefreshRunner = { command ->
                            IdeHelperRefreshResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                helperRoot = helperRoot,
                                updatedFiles = emptyList(),
                                classpathEntries = listOf(repoRoot.resolve("runtime/microsmith-cli-all.jar")),
                            )
                        },
                    )

                result.createdFiles.shouldContainExactly(listOf(repoRoot.resolve("settings.microsmith.kts")))
                result.overwrittenFiles shouldBe emptyList()
                result.preservedFiles.shouldContainExactly(listOf(existingBuild))
                existingBuild.readText() shouldBe "// existing build script"
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "preserves existing regular bootstrap files without reading content when force is disabled" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-preserve-bytes")
            val buildScript = repoRoot.resolve("build.microsmith.kts")
            val originalBytes = byteArrayOf(0xC3.toByte(), 0x28)
            Files.write(buildScript, originalBytes)
            try {
                val result =
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot, skipIdeHelper = true),
                    )

                result.createdFiles.shouldContainExactly(listOf(repoRoot.resolve("settings.microsmith.kts")))
                result.overwrittenFiles shouldBe emptyList()
                result.preservedFiles.shouldContainExactly(listOf(buildScript))
                Files.readAllBytes(buildScript).contentEquals(originalBytes) shouldBe true
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "overwrites existing regular bootstrap files when force is enabled" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-force")
            val buildScript = repoRoot.resolve("build.microsmith.kts")
            val settingsScript = repoRoot.resolve("settings.microsmith.kts")
            buildScript.writeText("// stale build script")
            settingsScript.writeText("// stale settings")
            repoRoot.resolve("go.mod").writeText("module example.com/microsmith/fixture\n")
            try {
                val helperRoot = repoRoot.resolve(".microsmith/ide")
                val result =
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot, force = true),
                        ideRefreshRunner = { command ->
                            IdeHelperRefreshResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                helperRoot = helperRoot,
                                updatedFiles = listOf(helperRoot.resolve("build.gradle.kts")),
                                classpathEntries = listOf(repoRoot.resolve("runtime/microsmith-cli-all.jar")),
                            )
                        },
                    )

                result.repositoryDetection.type shouldBe OnboardingRepositoryType.GO
                result.createdFiles shouldBe emptyList()
                result.overwrittenFiles.shouldContainExactly(listOf(buildScript, settingsScript))
                result.preservedFiles shouldBe emptyList()
                buildScript.readText().shouldContain("GoUserCreated")
                settingsScript.readText().shouldContain("go.mod")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "overwrites undecodable bootstrap files when force is enabled" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-force-invalid-bytes")
            val buildScript = repoRoot.resolve("build.microsmith.kts")
            Files.write(buildScript, byteArrayOf(0xC3.toByte(), 0x28))
            try {
                val result =
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot, force = true, skipIdeHelper = true),
                    )

                result.createdFiles.shouldContainExactly(listOf(repoRoot.resolve("settings.microsmith.kts")))
                result.overwrittenFiles.shouldContainExactly(listOf(buildScript))
                result.preservedFiles shouldBe emptyList()
                buildScript.readText().shouldContain("UserCreated")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "skips IDE helper refresh when explicitly disabled" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-skip-ide")
            try {
                val result =
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot, skipIdeHelper = true),
                        ideRefreshRunner = { error("IDE helper refresh should be skipped when disabled") },
                    )

                result.createdFiles.shouldHaveSize(2)
                result.ideHelperResult shouldBe null
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "rethrows IDE helper conflicts as init conflicts" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-ide-conflict")
            try {
                val error =
                    shouldThrow<InitConflictException> {
                        runInitBootstrap(
                            command = InitCommand(projectRoot = repoRoot),
                            ideRefreshRunner = {
                                throw IdeHelperConflictException("IDE helper path is invalid.")
                            },
                        )
                    }

                error.message shouldBe "IDE helper path is invalid."
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "ignores .NET marker traversal failures while detecting other repository markers" {
            val nodeRoot = createTempDirectory("microsmith-init-detect-node-traversal-failure")
            try {
                nodeRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")

                detectOnboardingRepositoryType(
                    projectRoot = nodeRoot,
                    dotnetMarkerFinder = { throw IOException("permission denied") },
                ) shouldBe
                    OnboardingRepositoryDetection(
                        type = OnboardingRepositoryType.NODE,
                        matchedMarkers = listOf("package.json"),
                    )
            } finally {
                runCatching { nodeRoot.deleteRecursively() }
            }
        }

        "ignores unchecked .NET marker traversal failures while detecting other repository markers" {
            val nodeRoot = createTempDirectory("microsmith-init-detect-node-unchecked-traversal-failure")
            try {
                nodeRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")

                detectOnboardingRepositoryType(
                    projectRoot = nodeRoot,
                    dotnetMarkerFinder = {
                        throw UncheckedIOException(IOException("permission denied"))
                    },
                ) shouldBe
                    OnboardingRepositoryDetection(
                        type = OnboardingRepositoryType.NODE,
                        matchedMarkers = listOf("package.json"),
                    )
            } finally {
                runCatching { nodeRoot.deleteRecursively() }
            }
        }

        "detects Node, Go, and .NET repositories and falls back to Other for mixed markers" {
            val nodeRoot = createTempDirectory("microsmith-init-detect-node")
            val goRoot = createTempDirectory("microsmith-init-detect-go")
            val dotnetRoot = createTempDirectory("microsmith-init-detect-dotnet")
            val mixedRoot = createTempDirectory("microsmith-init-detect-mixed")
            try {
                nodeRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")
                goRoot.resolve("go.mod").writeText("module example.com/microsmith/fixture\n")
                dotnetRoot.resolve("src/apps/service").createDirectories()
                dotnetRoot.resolve("src/apps/service/Fixture.csproj")
                    .writeText("<Project Sdk=\"Microsoft.NET.Sdk\" />\n")
                mixedRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")
                mixedRoot.resolve("go.mod").writeText("module example.com/microsmith/fixture\n")

                detectOnboardingRepositoryType(nodeRoot) shouldBe
                    OnboardingRepositoryDetection(
                        type = OnboardingRepositoryType.NODE,
                        matchedMarkers = listOf("package.json"),
                    )
                detectOnboardingRepositoryType(goRoot) shouldBe
                    OnboardingRepositoryDetection(
                        type = OnboardingRepositoryType.GO,
                        matchedMarkers = listOf("go.mod"),
                    )
                detectOnboardingRepositoryType(dotnetRoot) shouldBe
                    OnboardingRepositoryDetection(
                        type = OnboardingRepositoryType.DOTNET,
                        matchedMarkers = listOf("src/apps/service/Fixture.csproj"),
                    )
                detectOnboardingRepositoryType(mixedRoot) shouldBe
                    OnboardingRepositoryDetection(
                        type = OnboardingRepositoryType.OTHER,
                        matchedMarkers = listOf("go.mod", "package.json"),
                    )
            } finally {
                runCatching { nodeRoot.deleteRecursively() }
                runCatching { goRoot.deleteRecursively() }
                runCatching { dotnetRoot.deleteRecursively() }
                runCatching { mixedRoot.deleteRecursively() }
            }
        }

        "throws conflict when bootstrap path exists and is not a regular file" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-conflict")
            repoRoot.resolve("build.microsmith.kts").createDirectories()
            try {
                val error =
                    shouldThrow<InitConflictException> {
                        runInitBootstrap(
                            command = InitCommand(projectRoot = repoRoot),
                            ideRefreshRunner = { error("should not refresh IDE helper when conflict exists") },
                        )
                    }

                error.message.shouldContain("exists but is not a regular file")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "throws conflict when bootstrap path exists as a symlink".config(enabled = !runningOnWindows()) {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-symlink")
            val targetFile = createTempDirectory("microsmith-init-bootstrap-symlink-target")
                .resolve("external-build.microsmith.kts")
            targetFile.writeText("// external build script")
            Files.createSymbolicLink(repoRoot.resolve("build.microsmith.kts"), targetFile)
            try {
                val error =
                    shouldThrow<InitConflictException> {
                        runInitBootstrap(
                            command = InitCommand(projectRoot = repoRoot, force = true),
                            ideRefreshRunner = { error("should not refresh IDE helper when conflict exists") },
                        )
                    }

                error.message.shouldContain("exists but is not a regular file")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { targetFile.parent.deleteRecursively() }
            }
        }

        "throws validation error when repository root does not exist" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-missing")
            repoRoot.deleteRecursively()

            val error =
                shouldThrow<InitValidationException> {
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot),
                        ideRefreshRunner = { error("should not refresh IDE helper when root is missing") },
                    )
                }

            error.message.shouldContain("does not exist")
            repoRoot.exists() shouldBe false
        }
    })

private fun runningOnWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
