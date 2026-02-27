package me.liam.microsmith.cli.init

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
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
        "creates default bootstrap files and invokes IDE helper refresh" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-create")
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

                result.createdFiles.shouldHaveSize(2)
                result.createdFiles.shouldContain(repoRoot.resolve("build.microsmith.kts"))
                result.createdFiles.shouldContain(repoRoot.resolve("settings.microsmith.kts"))
                result.preservedFiles shouldBe emptyList()
                repoRoot.resolve("build.microsmith.kts").isRegularFile() shouldBe true
                repoRoot.resolve("settings.microsmith.kts").isRegularFile() shouldBe true
                repoRoot.resolve("build.microsmith.kts").readText().shouldContain("microsmith {")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "preserves existing bootstrap files on repeated init runs" {
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

                result.createdFiles.shouldHaveSize(1)
                result.createdFiles.single() shouldBe repoRoot.resolve("settings.microsmith.kts")
                result.preservedFiles.shouldContain(existingBuild)
                existingBuild.readText() shouldBe "// existing build script"
            } finally {
                runCatching { repoRoot.deleteRecursively() }
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

        "throws validation error when repository root does not exist" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-missing")
            repoRoot.deleteRecursively()

            val error =
                shouldThrow<IllegalArgumentException> {
                    runInitBootstrap(
                        command = InitCommand(projectRoot = repoRoot),
                        ideRefreshRunner = { error("should not refresh IDE helper when root is missing") },
                    )
                }

            error.message.shouldContain("does not exist")
            repoRoot.exists() shouldBe false
        }
    })
