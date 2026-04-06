package io.github.lmliam.microsmith.cli.ide

import io.github.lmliam.microsmith.cli.command.IdeRefreshCommand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class IdeHelperGeneratorTests :
    StringSpec({
        "generates helper files in .microsmith/ide" {
            val repoRoot = createTempDirectory("microsmith-ide-helper-generate")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            val runtimeClasses = repoRoot.resolve("runtime/runtime-classes")
            runtimeJar.parent.createDirectories()
            runtimeClasses.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")

            try {
                val result =
                    refreshIdeHelperProject(
                        command = IdeRefreshCommand(projectRoot = repoRoot),
                        classpathResolver = { listOf(runtimeJar, runtimeClasses) },
                    )

                result.updatedFiles.shouldHaveSize(3)
                result.classpathEntries.shouldContain(runtimeJar.toAbsolutePath().normalize())
                result.classpathEntries.shouldContain(runtimeClasses.toAbsolutePath().normalize())

                val settingsFile = repoRoot.resolve(".microsmith/ide/settings.gradle.kts")
                val buildFile = repoRoot.resolve(".microsmith/ide/build.gradle.kts")
                val readmeFile = repoRoot.resolve(".microsmith/ide/README.md")
                settingsFile.exists() shouldBe true
                buildFile.exists() shouldBe true
                readmeFile.exists() shouldBe true

                val buildContent = buildFile.readText()
                val runtimeJarLiteral = runtimeJar.toAbsolutePath().normalize().toString().replace('\\', '/')
                buildContent.shouldContain("id(\"java-library\")")
                buildContent.shouldContain("files(\n            \"")
                buildContent.shouldContain(runtimeJarLiteral)
                val helperReadme = readmeFile.readText()
                helperReadme.shouldContain("microsmith ide refresh")
                helperReadme.shouldContain("microsmith ide doctor --diagnostics json --verbose")
                helperReadme.shouldContain(".microsmith/ide/` to your repository `.gitignore`")
                helperReadme.shouldContain("do not hand-edit")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "is idempotent when helper files are already up to date" {
            val repoRoot = createTempDirectory("microsmith-ide-helper-idempotent")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            runtimeJar.parent.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")

            try {
                val command = IdeRefreshCommand(projectRoot = repoRoot)
                val first =
                    refreshIdeHelperProject(
                        command = command,
                        classpathResolver = { listOf(runtimeJar) },
                    )
                val second =
                    refreshIdeHelperProject(
                        command = command,
                        classpathResolver = { listOf(runtimeJar) },
                    )

                first.updatedFiles.shouldHaveSize(3)
                second.updatedFiles shouldBe emptyList()
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "fails when classpath resolution yields no usable entries" {
            val repoRoot = createTempDirectory("microsmith-ide-helper-empty-classpath")
            try {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        refreshIdeHelperProject(
                            command = IdeRefreshCommand(projectRoot = repoRoot),
                            classpathResolver = { emptyList() },
                        )
                    }

                error.message.shouldContain("Could not resolve runtime classpath entries")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "resolves classpath entries from java.class.path text deterministically" {
            val separator = File.pathSeparator
            val entries = resolveIdeHelperClasspathEntries("/tmp/one.jar$separator/tmp/two.jar")
            entries.shouldHaveSize(2)
            entries.shouldContain(Path.of("/tmp/one.jar"))
            entries.shouldContain(Path.of("/tmp/two.jar"))
        }

        "escapes dollar signs in generated classpath literals" {
            val repoRoot = createTempDirectory("microsmith-ide-helper-path-escaping")
            val runtimeJar = repoRoot.resolve($$"runtime/dollar$name.jar")
            runtimeJar.parent.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")

            try {
                refreshIdeHelperProject(
                    command = IdeRefreshCommand(projectRoot = repoRoot),
                    classpathResolver = { listOf(runtimeJar) },
                )

                val buildFile = repoRoot.resolve(".microsmith/ide/build.gradle.kts")
                buildFile.readText().shouldContain($$"dollar\\$name.jar")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "overwrites undecodable managed helper files" {
            val repoRoot = createTempDirectory("microsmith-ide-helper-invalid-bytes")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            val helperRoot = repoRoot.resolve(".microsmith/ide")
            runtimeJar.parent.createDirectories()
            helperRoot.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")
            Files.write(helperRoot.resolve("build.gradle.kts"), byteArrayOf(0xC3.toByte(), 0x28))

            try {
                val result =
                    refreshIdeHelperProject(
                        command = IdeRefreshCommand(projectRoot = repoRoot),
                        classpathResolver = { listOf(runtimeJar) },
                    )

                result.updatedFiles.shouldContain(helperRoot.resolve("build.gradle.kts"))
                helperRoot.resolve("build.gradle.kts").readText().shouldContain("java-library")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "fails when managed helper root exists as a symlink".config(enabled = !runningOnWindows()) {
            val repoRoot = createTempDirectory("microsmith-ide-helper-root-symlink")
            val externalRoot = createTempDirectory("microsmith-ide-helper-root-symlink-target")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            runtimeJar.parent.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")
            Files.createSymbolicLink(repoRoot.resolve(".microsmith"), externalRoot)

            try {
                val error =
                    shouldThrow<IdeHelperConflictException> {
                        refreshIdeHelperProject(
                            command = IdeRefreshCommand(projectRoot = repoRoot),
                            classpathResolver = { listOf(runtimeJar) },
                        )
                    }

                error.message.shouldContain("exists but is not a directory")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { externalRoot.deleteRecursively() }
            }
        }

        "fails when managed helper file exists as a symlink".config(enabled = !runningOnWindows()) {
            val repoRoot = createTempDirectory("microsmith-ide-helper-file-symlink")
            val externalRoot = createTempDirectory("microsmith-ide-helper-file-symlink-target")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            val helperRoot = repoRoot.resolve(".microsmith/ide")
            val targetFile = externalRoot.resolve("external-build.gradle.kts")
            runtimeJar.parent.createDirectories()
            helperRoot.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")
            targetFile.writeText("// external build file")
            Files.createSymbolicLink(helperRoot.resolve("build.gradle.kts"), targetFile)

            try {
                val error =
                    shouldThrow<IdeHelperConflictException> {
                        refreshIdeHelperProject(
                            command = IdeRefreshCommand(projectRoot = repoRoot),
                            classpathResolver = { listOf(runtimeJar) },
                        )
                    }

                error.message.shouldContain("exists but is not a regular file")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { externalRoot.deleteRecursively() }
            }
        }
    })

private fun runningOnWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
