package io.github.lmliam.microsmith.cli.init

import io.github.lmliam.microsmith.cli.command.InitCommand
import io.github.lmliam.microsmith.cli.ide.IdeHelperRefreshResult
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class JavaOnboardingProfileTests :
    StringSpec({
        "creates repo-aware bootstrap files for Java repositories without a repository-native output override" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-java")
            repoRoot.resolve("pom.xml").writeText("<project />\n")
            repoRoot.resolve("src/main/java/example").createDirectories()
            repoRoot.resolve("src/main/java/example/App.java").writeText(
                """
                package example;

                public final class App {
                    public String message() {
                        return "Microsmith Java fixture";
                    }
                }
                """.trimIndent() + "\n",
            )
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

                result.repositoryDetection.profile shouldBe JavaOnboardingProfile
                result.repositoryDetection.matchedMarkers shouldBe listOf("pom.xml", "src/main/java")
                val buildScript = repoRoot.resolve("build.microsmith.kts").readText()
                val settingsScript = repoRoot.resolve("settings.microsmith.kts").readText()

                buildScript.shouldContain("JavaUserCreated")
                buildScript.shouldContain("microsmith run build.microsmith.kts --out ./generated")
                buildScript.shouldContain("// Bootstrapped Microsmith schema for this Java repository.")
                buildScript.contains("Common repository-native output path:") shouldBe false
                settingsScript.shouldContain("Detected repository profile: Java")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Java repositories from Maven roots when a Java source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-java-maven")
            try {
                repoRoot.resolve("pom.xml").writeText("<project />\n")
                repoRoot.resolve("src/main/java/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = JavaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("pom.xml", "src/main/java"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects multi-module Maven Java repositories from nested Java source trees" {
            val repoRoot = createTempDirectory("microsmith-init-detect-java-maven-multi-module")
            try {
                repoRoot.resolve("pom.xml").writeText("<project />\n")
                repoRoot.resolve("modules/service-a/src/main/java/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = JavaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("modules/service-a/src/main/java", "pom.xml"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Java repositories from Gradle roots when a Java source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-java-gradle")
            try {
                repoRoot.resolve("build.gradle.kts").writeText("plugins { java }\n")
                repoRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture-java\"\n")
                repoRoot.resolve("src/test/java/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = JavaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle.kts", "settings.gradle.kts", "src/test/java"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects multi-module Gradle Java repositories from nested Java source trees" {
            val repoRoot = createTempDirectory("microsmith-init-detect-java-gradle-multi-module")
            try {
                repoRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture-java\"\n")
                repoRoot.resolve("services/app/src/test/java/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = JavaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("services/app/src/test/java", "settings.gradle.kts"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects build-tool-light Java repositories from source roots alone" {
            val repoRoot = createTempDirectory("microsmith-init-detect-java-lightweight")
            try {
                repoRoot.resolve("src/main/java/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = JavaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("src/main/java"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "falls back to the generic profile for Java build files without a Java source tree" {
            val repoRoot = createTempDirectory("microsmith-init-detect-java-build-only")
            try {
                repoRoot.resolve("pom.xml").writeText("<project />\n")
                repoRoot.resolve("build.gradle.kts").writeText("plugins { java }\n")

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                        matchedMarkers = emptyList(),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "falls back to the generic profile when Java and another ecosystem marker both match" {
            val repoRoot = createTempDirectory("microsmith-init-detect-java-mixed")
            try {
                repoRoot.resolve("pom.xml").writeText("<project />\n")
                repoRoot.resolve("src/main/java/example").createDirectories()
                repoRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                        matchedMarkers = listOf("package.json", "pom.xml", "src/main/java"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }
    })
