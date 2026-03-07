package me.liam.microsmith.cli.ide

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.command.IdeDoctorCommand
import me.liam.microsmith.cli.command.IdeRefreshCommand
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class IdeHelperDoctorTests :
    StringSpec({
        "passes when helper files exist and classpath is synchronized" {
            val repoRoot = createTempDirectory("microsmith-ide-doctor-pass")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            runtimeJar.parent.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")
            try {
                refreshIdeHelperProject(
                    command = IdeRefreshCommand(projectRoot = repoRoot),
                    classpathResolver = { listOf(runtimeJar) },
                )

                val result =
                    runIdeHelperDoctor(
                        command = IdeDoctorCommand(projectRoot = repoRoot),
                        classpathResolver = { listOf(runtimeJar) },
                    )

                result.hasFailures shouldBe false
                result.checks.filter { !it.passed } shouldBe emptyList()
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "reports missing helper directory when ide refresh has not been run" {
            val repoRoot = createTempDirectory("microsmith-ide-doctor-missing-helper")
            try {
                val result =
                    runIdeHelperDoctor(
                        command = IdeDoctorCommand(projectRoot = repoRoot),
                        classpathResolver = { emptyList() },
                    )

                result.hasFailures shouldBe true
                result.checks.map { check -> check.id }.shouldContain("helper-directory")
                result.checks.first { check -> check.id == "helper-directory" }.message.shouldContain("missing")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "reports stale classpath when helper build file does not include current runtime jars" {
            val repoRoot = createTempDirectory("microsmith-ide-doctor-stale-classpath")
            val runtimeJarV1 = repoRoot.resolve("runtime/microsmith-cli-v1.jar")
            val runtimeJarV2 = repoRoot.resolve("runtime/microsmith-cli-v2.jar")
            runtimeJarV1.parent.createDirectories()
            runtimeJarV1.writeText("jar-binary-placeholder-v1")
            runtimeJarV2.writeText("jar-binary-placeholder-v2")
            try {
                refreshIdeHelperProject(
                    command = IdeRefreshCommand(projectRoot = repoRoot),
                    classpathResolver = { listOf(runtimeJarV1) },
                )

                val result =
                    runIdeHelperDoctor(
                        command = IdeDoctorCommand(projectRoot = repoRoot),
                        classpathResolver = { listOf(runtimeJarV2) },
                    )

                result.hasFailures shouldBe true
                val classpathSyncCheck = result.checks.first { check -> check.id == "classpath-sync" }
                classpathSyncCheck.passed shouldBe false
                classpathSyncCheck.message.shouldContain("stale")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "reports conflicting managed helper files" {
            val repoRoot = createTempDirectory("microsmith-ide-doctor-conflicting-helper")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            val helperRoot = repoRoot.resolve(".microsmith/ide")
            runtimeJar.parent.createDirectories()
            helperRoot.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")
            helperRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"microsmith-ide-helper\"")
            helperRoot.resolve("README.md").writeText("# helper")
            helperRoot.resolve("build.gradle.kts").createDirectories()
            try {
                val result =
                    runIdeHelperDoctor(
                        command = IdeDoctorCommand(projectRoot = repoRoot),
                        classpathResolver = { listOf(runtimeJar) },
                    )

                result.hasFailures shouldBe true
                val requiredFilesCheck = result.checks.first { check -> check.id == "required-files" }
                requiredFilesCheck.passed shouldBe false
                requiredFilesCheck.message.shouldContain("conflicting managed paths")
                requiredFilesCheck.details["invalidFiles"]
                    .shouldContain(helperRoot.resolve("build.gradle.kts").toString())
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "reports stale helper build file when managed file is undecodable" {
            val repoRoot = createTempDirectory("microsmith-ide-doctor-invalid-bytes")
            val runtimeJar = repoRoot.resolve("runtime/microsmith-cli-all.jar")
            val helperRoot = repoRoot.resolve(".microsmith/ide")
            runtimeJar.parent.createDirectories()
            runtimeJar.writeText("jar-binary-placeholder")
            try {
                refreshIdeHelperProject(
                    command = IdeRefreshCommand(projectRoot = repoRoot),
                    classpathResolver = { listOf(runtimeJar) },
                )
                Files.write(helperRoot.resolve("build.gradle.kts"), byteArrayOf(0xC3.toByte(), 0x28))

                val result =
                    runIdeHelperDoctor(
                        command = IdeDoctorCommand(projectRoot = repoRoot),
                        classpathResolver = { listOf(runtimeJar) },
                    )

                result.hasFailures shouldBe true
                val classpathSyncCheck = result.checks.first { check -> check.id == "classpath-sync" }
                classpathSyncCheck.passed shouldBe false
                classpathSyncCheck.message.shouldContain("stale")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }
    })
