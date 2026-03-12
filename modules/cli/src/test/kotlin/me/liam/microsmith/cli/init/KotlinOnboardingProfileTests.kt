package me.liam.microsmith.cli.init

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class KotlinOnboardingProfileTests :
    StringSpec({
        "creates repo-aware bootstrap files for Kotlin repositories without a repository-native output override" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-kotlin")
            repoRoot.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"2.2.21\" }\n")
            repoRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture-kotlin\"\n")
            repoRoot.resolve("src/main/kotlin/example").createDirectories()
            repoRoot.resolve("src/main/kotlin/example/App.kt").writeText(
                """
                package example

                class App {
                    fun message(): String = "Microsmith Kotlin fixture"
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

                result.repositoryDetection.profile shouldBe KotlinOnboardingProfile
                result.repositoryDetection.matchedMarkers shouldBe
                    listOf("build.gradle.kts", "settings.gradle.kts", "src/main/kotlin")
                val buildScript = repoRoot.resolve("build.microsmith.kts").readText()
                val settingsScript = repoRoot.resolve("settings.microsmith.kts").readText()

                buildScript.shouldContain("KotlinUserCreated")
                buildScript.shouldContain("microsmith run build.microsmith.kts --out ./generated")
                buildScript.shouldContain("// Bootstrapped Microsmith schema for this Kotlin repository.")
                buildScript.contains("Common repository-native output path:") shouldBe false
                settingsScript.shouldContain("Detected repository profile: Kotlin")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Kotlin repositories from Maven roots when a Kotlin source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-maven")
            try {
                repoRoot.resolve("pom.xml").writeText("<project />\n")
                repoRoot.resolve("src/main/kotlin/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("pom.xml", "src/main/kotlin"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Kotlin repositories from Gradle Kotlin DSL roots when a Kotlin source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-gradle-kts")
            try {
                repoRoot.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"2.2.21\" }\n")
                repoRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture-kotlin\"\n")
                repoRoot.resolve("src/test/kotlin/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle.kts", "settings.gradle.kts", "src/test/kotlin"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Kotlin repositories from Gradle Groovy roots when a Kotlin source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-gradle-groovy")
            try {
                repoRoot.resolve(
                    "build.gradle",
                ).writeText("plugins { id 'org.jetbrains.kotlin.jvm' version '2.2.21' }\n")
                repoRoot.resolve(
                    "settings.gradle",
                ).writeText("rootProject.name = 'fixture-kotlin'\n")
                repoRoot.resolve("src/main/kotlin/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle", "settings.gradle", "src/main/kotlin"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Kotlin multiplatform repositories from source-set roots" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-multiplatform")
            try {
                repoRoot.resolve(
                    "build.gradle.kts",
                ).writeText("plugins { kotlin(\"multiplatform\") version \"2.2.21\" }\n")
                repoRoot.resolve(
                    "src/commonMain/kotlin/example",
                ).createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle.kts", "src/commonMain/kotlin"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects build-tool-light Kotlin repositories from source roots alone" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-lightweight")
            try {
                repoRoot.resolve("src/main/kotlin/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("src/main/kotlin"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects multi-module Kotlin repositories from nested Kotlin source trees" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-multi-module")
            try {
                repoRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture-kotlin\"\n")
                repoRoot.resolve("services/app/src/jvmMain/kotlin/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("services/app/src/jvmMain/kotlin", "settings.gradle.kts"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "falls back to the generic profile for Kotlin build files without a Kotlin source tree" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-build-only")
            try {
                repoRoot.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"2.2.21\" }\n")
                repoRoot.resolve("pom.xml").writeText("<project />\n")

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

        "falls back to the generic profile when Kotlin and another ecosystem marker both match" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-mixed")
            try {
                repoRoot.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"2.2.21\" }\n")
                repoRoot.resolve("src/main/kotlin/example").createDirectories()
                repoRoot.resolve("pyproject.toml").writeText("[project]\nname = \"fixture-python\"\n")

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                        matchedMarkers = listOf("build.gradle.kts", "pyproject.toml", "src/main/kotlin"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "falls back to the generic profile when Java and Kotlin source roots both match" {
            val repoRoot = createTempDirectory("microsmith-init-detect-kotlin-java-mixed")
            try {
                repoRoot.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"2.2.21\" }\n")
                repoRoot.resolve("src/main/java/example").createDirectories()
                repoRoot.resolve("src/main/kotlin/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                        matchedMarkers = listOf("build.gradle.kts", "src/main/java", "src/main/kotlin"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }
    })
