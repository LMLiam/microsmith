package io.github.lmliam.microsmith.cli

import io.github.lmliam.microsmith.cli.command.InitCommand
import io.github.lmliam.microsmith.cli.ide.IdeHelperRefreshResult
import io.github.lmliam.microsmith.cli.init.InitBootstrapResult
import io.github.lmliam.microsmith.cli.init.profile.JavaOnboardingProfile
import io.github.lmliam.microsmith.cli.init.profile.KotlinOnboardingProfile
import io.github.lmliam.microsmith.cli.init.profile.OnboardingProfile
import io.github.lmliam.microsmith.cli.init.profile.OnboardingProfileDetection
import io.github.lmliam.microsmith.cli.init.profile.OnboardingProfileSelectionReason
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path as NioPath
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class MicrosmithCliInitNativeJvmGuidanceTests :
    StringSpec({
        "init command steers Maven-based Java repositories to the native Maven plugin path" {
            assertNativeJvmGuidance(
                profile = JavaOnboardingProfile,
                matchedMarkers = listOf("pom.xml", "src/main/java"),
                expectedOutput =
                    listOf(
                        "Detected repository profile: Java",
                        "Next: microsmith run build.microsmith.kts",
                        "Prefer the native Maven plugin path",
                        "mvn microsmith:generate",
                    ),
                unexpectedOutput = listOf("Optional repository-native output path"),
                tempDirectoryPrefix = "microsmith-cli-init-java-maven",
            )
        }

        "init command points Maven-based Kotlin repositories toward the native Maven plugin path" {
            assertNativeJvmGuidance(
                profile = KotlinOnboardingProfile,
                matchedMarkers = listOf("pom.xml", "src/main/kotlin"),
                expectedOutput =
                    listOf(
                        "Detected repository profile: Kotlin",
                        "Prefer the native Maven plugin path",
                        "mvn microsmith:generate",
                    ),
                tempDirectoryPrefix = "microsmith-cli-init-kotlin-maven",
            )
        }

        "init command points Gradle-based Kotlin repositories toward the native Gradle plugin path" {
            assertNativeJvmGuidance(
                profile = KotlinOnboardingProfile,
                matchedMarkers = listOf("build.gradle.kts", "src/main/kotlin"),
                expectedOutput =
                    listOf(
                        "Detected repository profile: Kotlin",
                        "Next: microsmith run build.microsmith.kts",
                        "Prefer the native Gradle plugin path",
                    ),
                unexpectedOutput = listOf("Optional repository-native output path"),
                tempDirectoryPrefix = "microsmith-cli-init-kotlin-gradle",
            )
        }

        "init command points Gradle-based Java repositories toward the native Gradle plugin path" {
            assertNativeJvmGuidance(
                profile = JavaOnboardingProfile,
                matchedMarkers = listOf("build.gradle.kts", "src/main/java"),
                expectedOutput =
                    listOf(
                        "Detected repository profile: Java",
                        "Prefer the native Gradle plugin path",
                    ),
                tempDirectoryPrefix = "microsmith-cli-init-java-gradle",
            )
        }
    })

@OptIn(ExperimentalPathApi::class)
private fun assertNativeJvmGuidance(
    profile: OnboardingProfile,
    matchedMarkers: List<String>,
    expectedOutput: List<String>,
    unexpectedOutput: List<String> = emptyList(),
    tempDirectoryPrefix: String,
) {
    val out = mutableListOf<String>()
    val err = mutableListOf<String>()
    val tempDir = createTempDirectory(tempDirectoryPrefix)
    try {
        val cli =
            MicrosmithCli(
                stdout = out::add,
                stderr = err::add,
                initRunner = { command: InitCommand ->
                    nativeJvmBootstrapResult(command, tempDir, profile, matchedMarkers)
                },
            )

        val exitCode = cli.run(arrayOf("init", "--repo-root", tempDir.toString()))

        exitCode shouldBe 0
        val output = out.joinToString("\n")
        expectedOutput.forEach(output::shouldContain)
        unexpectedOutput.forEach(output::shouldNotContain)
        err shouldBe emptyList()
    } finally {
        runCatching { tempDir.deleteRecursively() }
    }
}

private fun nativeJvmBootstrapResult(
    command: InitCommand,
    projectRoot: NioPath,
    profile: OnboardingProfile,
    matchedMarkers: List<String>,
): InitBootstrapResult {
    val helperRoot = projectRoot.resolve(".microsmith/ide")
    return InitBootstrapResult(
        projectRoot = command.projectRoot.toAbsolutePath().normalize(),
        repositoryDetection =
            OnboardingProfileDetection(
                profile = profile,
                selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                matchedMarkers = matchedMarkers,
            ),
        createdFiles = listOf(projectRoot.resolve("build.microsmith.kts")),
        overwrittenFiles = emptyList(),
        preservedFiles = emptyList(),
        ideHelperResult =
            IdeHelperRefreshResult(
                projectRoot = projectRoot,
                helperRoot = helperRoot,
                updatedFiles = emptyList(),
                classpathEntries = listOf(projectRoot.resolve("microsmith-cli-all.jar")),
            ),
    )
}
