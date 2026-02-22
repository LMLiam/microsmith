package me.liam.microsmith.cli.ide

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.command.IdeRefreshCommand
import java.io.File
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
                readmeFile.readText().shouldContain("microsmith ide refresh")
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
            val runtimeJar = repoRoot.resolve("runtime/dollar\$name.jar")
            runtimeJar.parent.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")

            try {
                refreshIdeHelperProject(
                    command = IdeRefreshCommand(projectRoot = repoRoot),
                    classpathResolver = { listOf(runtimeJar) },
                )

                val buildFile = repoRoot.resolve(".microsmith/ide/build.gradle.kts")
                buildFile.readText().shouldContain("dollar\\\$name.jar")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }
    })
