package io.github.lmliam.microsmith.cli.doctor

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class DoctorRunnerTests :
    StringSpec({
        "bootstrap-state passes when init has not been run in the working directory" {
            val repoRoot = createTempDirectory("microsmith-doctor-bootstrap-absent")
            val scriptCache = createTempDirectory("microsmith-doctor-script-cache")
            val pluginCache = createTempDirectory("microsmith-doctor-plugin-cache")
            try {
                val result =
                    runDoctorChecks(
                        providerValidator = { emptyList() },
                        scriptCacheDirectory = scriptCache,
                        pluginCacheDirectory = pluginCache,
                        projectRoot = repoRoot,
                    )

                val bootstrapCheck = result.checks.single { it.id == "bootstrap-state" }
                bootstrapCheck.status shouldBe DoctorCheckStatus.PASS
                bootstrapCheck.message.shouldContain("were not detected")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { scriptCache.deleteRecursively() }
                runCatching { pluginCache.deleteRecursively() }
            }
        }

        "bootstrap-state fails when bootstrap scripts are incomplete" {
            val repoRoot = createTempDirectory("microsmith-doctor-bootstrap-incomplete")
            val scriptCache = createTempDirectory("microsmith-doctor-script-cache")
            val pluginCache = createTempDirectory("microsmith-doctor-plugin-cache")
            repoRoot.resolve("build.microsmith.kts").writeText("microsmith { }")
            try {
                val result =
                    runDoctorChecks(
                        providerValidator = { emptyList() },
                        scriptCacheDirectory = scriptCache,
                        pluginCacheDirectory = pluginCache,
                        projectRoot = repoRoot,
                    )

                val bootstrapCheck = result.checks.single { it.id == "bootstrap-state" }
                bootstrapCheck.status shouldBe DoctorCheckStatus.FAIL
                bootstrapCheck.message.shouldContain("Run 'microsmith init'")
                bootstrapCheck.details["missingBootstrapFiles"] shouldBe "settings.microsmith.kts"
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { scriptCache.deleteRecursively() }
                runCatching { pluginCache.deleteRecursively() }
            }
        }

        "bootstrap-state fails when bootstrap paths conflict with managed files" {
            val repoRoot = createTempDirectory("microsmith-doctor-bootstrap-conflict")
            val scriptCache = createTempDirectory("microsmith-doctor-script-cache")
            val pluginCache = createTempDirectory("microsmith-doctor-plugin-cache")
            repoRoot.resolve("build.microsmith.kts").createDirectories()
            repoRoot.resolve("settings.microsmith.kts").writeText("// settings")
            try {
                val result =
                    runDoctorChecks(
                        providerValidator = { emptyList() },
                        scriptCacheDirectory = scriptCache,
                        pluginCacheDirectory = pluginCache,
                        projectRoot = repoRoot,
                    )

                val bootstrapCheck = result.checks.single { it.id == "bootstrap-state" }
                bootstrapCheck.status shouldBe DoctorCheckStatus.FAIL
                bootstrapCheck.message.shouldContain("conflicting paths")
                bootstrapCheck.details["invalidBootstrapFiles"] shouldBe "build.microsmith.kts"
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { scriptCache.deleteRecursively() }
                runCatching { pluginCache.deleteRecursively() }
            }
        }

        "bootstrap-state fails when the IDE helper is incomplete" {
            val repoRoot = createTempDirectory("microsmith-doctor-bootstrap-helper-stale")
            val scriptCache = createTempDirectory("microsmith-doctor-script-cache")
            val pluginCache = createTempDirectory("microsmith-doctor-plugin-cache")
            repoRoot.resolve("build.microsmith.kts").writeText("microsmith { }")
            repoRoot.resolve("settings.microsmith.kts").writeText("// settings")
            repoRoot.resolve(".microsmith/ide").createDirectories()
            repoRoot.resolve(".microsmith/ide/settings.gradle.kts")
                .writeText("rootProject.name = \"microsmith-ide-helper\"")
            try {
                val result =
                    runDoctorChecks(
                        providerValidator = { emptyList() },
                        scriptCacheDirectory = scriptCache,
                        pluginCacheDirectory = pluginCache,
                        projectRoot = repoRoot,
                    )

                val bootstrapCheck = result.checks.single { it.id == "bootstrap-state" }
                bootstrapCheck.status shouldBe DoctorCheckStatus.FAIL
                bootstrapCheck.message.shouldContain("microsmith ide refresh")
                bootstrapCheck.details["missingIdeHelperFiles"] shouldBe
                    ".microsmith/ide/README.md,.microsmith/ide/build.gradle.kts"
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { scriptCache.deleteRecursively() }
                runCatching { pluginCache.deleteRecursively() }
            }
        }

        "bootstrap-state fails when the IDE helper contains conflicting managed paths" {
            val repoRoot = createTempDirectory("microsmith-doctor-bootstrap-helper-conflict")
            val scriptCache = createTempDirectory("microsmith-doctor-script-cache")
            val pluginCache = createTempDirectory("microsmith-doctor-plugin-cache")
            val helperRoot = repoRoot.resolve(".microsmith/ide")
            repoRoot.resolve("build.microsmith.kts").writeText("microsmith { }")
            repoRoot.resolve("settings.microsmith.kts").writeText("// settings")
            helperRoot.createDirectories()
            helperRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"microsmith-ide-helper\"")
            helperRoot.resolve("README.md").writeText("# helper")
            helperRoot.resolve("build.gradle.kts").createDirectories()
            try {
                val result =
                    runDoctorChecks(
                        providerValidator = { emptyList() },
                        scriptCacheDirectory = scriptCache,
                        pluginCacheDirectory = pluginCache,
                        projectRoot = repoRoot,
                    )

                val bootstrapCheck = result.checks.single { it.id == "bootstrap-state" }
                bootstrapCheck.status shouldBe DoctorCheckStatus.FAIL
                bootstrapCheck.message.shouldContain("conflicting managed paths")
                bootstrapCheck.details["invalidIdeHelperFiles"] shouldBe ".microsmith/ide/build.gradle.kts"
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { scriptCache.deleteRecursively() }
                runCatching { pluginCache.deleteRecursively() }
            }
        }

        "bootstrap-state fails when bootstrap files are present but IDE helper is missing" {
            val repoRoot = createTempDirectory("microsmith-doctor-bootstrap-no-ide")
            val scriptCache = createTempDirectory("microsmith-doctor-script-cache")
            val pluginCache = createTempDirectory("microsmith-doctor-plugin-cache")
            repoRoot.resolve("build.microsmith.kts").writeText("microsmith { }")
            repoRoot.resolve("settings.microsmith.kts").writeText("// settings")
            try {
                val result =
                    runDoctorChecks(
                        providerValidator = { emptyList() },
                        scriptCacheDirectory = scriptCache,
                        pluginCacheDirectory = pluginCache,
                        projectRoot = repoRoot,
                    )

                val bootstrapCheck = result.checks.single { it.id == "bootstrap-state" }
                bootstrapCheck.status shouldBe DoctorCheckStatus.FAIL
                bootstrapCheck.message.shouldContain("missing")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { scriptCache.deleteRecursively() }
                runCatching { pluginCache.deleteRecursively() }
            }
        }

        "bootstrap-state treats symlinked bootstrap files as invalid".config(enabled = !runningOnWindows()) {
            val repoRoot = createTempDirectory("microsmith-doctor-bootstrap-symlink")
            val targetRoot = createTempDirectory("microsmith-doctor-bootstrap-symlink-target")
            val scriptCache = createTempDirectory("microsmith-doctor-script-cache")
            val pluginCache = createTempDirectory("microsmith-doctor-plugin-cache")
            val externalBuildScript = targetRoot.resolve("build.microsmith.kts")
            externalBuildScript.writeText("microsmith { }")
            Files.createSymbolicLink(repoRoot.resolve("build.microsmith.kts"), externalBuildScript)
            repoRoot.resolve("settings.microsmith.kts").writeText("// settings")
            try {
                val result =
                    runDoctorChecks(
                        providerValidator = { emptyList() },
                        scriptCacheDirectory = scriptCache,
                        pluginCacheDirectory = pluginCache,
                        projectRoot = repoRoot,
                    )

                val bootstrapCheck = result.checks.single { it.id == "bootstrap-state" }
                bootstrapCheck.status shouldBe DoctorCheckStatus.FAIL
                bootstrapCheck.message.shouldContain("Run 'microsmith init'")
                bootstrapCheck.details["invalidBootstrapFiles"] shouldBe "build.microsmith.kts"
            } finally {
                runCatching { repoRoot.deleteRecursively() }
                runCatching { targetRoot.deleteRecursively() }
                runCatching { scriptCache.deleteRecursively() }
                runCatching { pluginCache.deleteRecursively() }
            }
        }
    })

private fun runningOnWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
