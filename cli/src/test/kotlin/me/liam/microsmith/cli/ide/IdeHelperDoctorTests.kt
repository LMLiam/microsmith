package me.liam.microsmith.cli.ide

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.command.IdeDoctorCommand
import me.liam.microsmith.cli.command.IdeRefreshCommand
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
    })
