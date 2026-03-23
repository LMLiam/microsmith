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
class ScalaOnboardingProfileTests :
    StringSpec({
        "creates repo-aware bootstrap files for Scala repositories without a repository-native output override" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-scala")
            repoRoot.resolve("project").createDirectories()
            repoRoot.resolve("build.sbt").writeText("""scalaVersion := "3.7.1""" + "\n")
            repoRoot.resolve("project/build.properties").writeText("sbt.version=1.11.7\n")
            repoRoot.resolve("src/main/scala/example").createDirectories()
            repoRoot.resolve("src/main/scala/example/App.scala").writeText(
                """
                package example

                object App {
                  def message: String = "Microsmith Scala fixture"
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

                result.repositoryDetection.profile shouldBe ScalaOnboardingProfile
                result.repositoryDetection.matchedMarkers shouldBe
                    listOf("build.sbt", "project/build.properties", "src/main/scala")
                val buildScript = repoRoot.resolve("build.microsmith.kts").readText()
                val settingsScript = repoRoot.resolve("settings.microsmith.kts").readText()

                buildScript.shouldContain("ScalaUserCreated")
                buildScript.shouldContain("microsmith run build.microsmith.kts")
                buildScript.shouldContain("// Bootstrapped Microsmith schema for this Scala repository.")
                buildScript.contains("Common repository-native output path:") shouldBe false
                settingsScript.shouldContain("Detected repository profile: Scala")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Scala repositories from sbt roots when a Scala main source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-sbt")
            try {
                repoRoot.resolve("project").createDirectories()
                repoRoot.resolve("build.sbt").writeText("""scalaVersion := "3.7.1""" + "\n")
                repoRoot.resolve("project/build.properties").writeText("sbt.version=1.11.7\n")
                repoRoot.resolve("src/main/scala/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = ScalaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.sbt", "project/build.properties", "src/main/scala"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Scala repositories from Maven roots when a Scala main source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-maven")
            try {
                repoRoot.resolve("pom.xml").writeText("<project />\n")
                repoRoot.resolve("src/main/scala/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = ScalaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("pom.xml", "src/main/scala"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Scala repositories from Gradle roots when a Scala main source tree exists" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-gradle")
            try {
                repoRoot.resolve("build.gradle.kts").writeText("plugins { scala }\n")
                repoRoot.resolve("settings.gradle.kts").writeText("rootProject.name = \"fixture-scala\"\n")
                repoRoot.resolve("src/main/scala/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = ScalaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle.kts", "settings.gradle.kts", "src/main/scala"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects multi-module Scala repositories from nested Scala main source trees" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-multi-module")
            try {
                repoRoot.resolve("build.sbt").writeText("""scalaVersion := "3.7.1""" + "\n")
                repoRoot.resolve("services/app/src/main/scala/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = ScalaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.sbt", "services/app/src/main/scala"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects build-tool-light Scala repositories from a Scala main source root alone" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-lightweight")
            try {
                repoRoot.resolve("src/main/scala/example").createDirectories()

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = ScalaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("src/main/scala"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "falls back to the generic profile for Scala build files without a Scala main source tree" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-build-only")
            try {
                repoRoot.resolve("project").createDirectories()
                repoRoot.resolve("build.sbt").writeText("""scalaVersion := "3.7.1""" + "\n")
                repoRoot.resolve("project/build.properties").writeText("sbt.version=1.11.7\n")

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

        "falls back to the generic profile for test-only Scala repositories" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-test-only")
            try {
                repoRoot.resolve("build.sbt").writeText("""scalaVersion := "3.7.1""" + "\n")
                repoRoot.resolve("src/test/scala/example").createDirectories()

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

        "falls back to the generic profile when Scala and another ecosystem marker both match" {
            val repoRoot = createTempDirectory("microsmith-init-detect-scala-mixed")
            try {
                repoRoot.resolve("build.sbt").writeText("""scalaVersion := "3.7.1""" + "\n")
                repoRoot.resolve("src/main/scala/example").createDirectories()
                repoRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                        matchedMarkers = listOf("build.sbt", "package.json", "src/main/scala"),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }
    })
