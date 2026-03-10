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
class RubyOnboardingProfileTests :
    StringSpec({
        "creates repo-aware bootstrap files for Ruby repositories without a repository-native output override" {
            val repoRoot = createTempDirectory("microsmith-init-bootstrap-ruby")
            repoRoot.resolve("Gemfile").writeText(
                """
                source "https://rubygems.org"
                gemspec
                """.trimIndent() + "\n",
            )
            repoRoot.resolve("microsmith-ruby-fixture.gemspec").writeText(
                """
                Gem::Specification.new do |spec|
                  spec.name = "microsmith-ruby-fixture"
                  spec.version = "0.1.0"
                  spec.summary = "Microsmith Ruby fixture"
                end
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

                result.repositoryDetection.profile shouldBe RubyOnboardingProfile
                result.repositoryDetection.matchedMarkers shouldBe listOf("Gemfile", "microsmith-ruby-fixture.gemspec")
                val buildScript = repoRoot.resolve("build.microsmith.kts").readText()
                val settingsScript = repoRoot.resolve("settings.microsmith.kts").readText()

                buildScript.shouldContain("RubyUserCreated")
                buildScript.shouldContain("microsmith run build.microsmith.kts --out ./generated")
                buildScript.shouldContain("// Bootstrapped Microsmith schema for this Ruby repository.")
                buildScript.shouldContain("Canonical first run:")
                buildScript.contains("Common repository-native output path:") shouldBe false
                settingsScript.shouldContain("Detected repository profile: Ruby")
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "detects Ruby repositories from Gemfile" {
            val rubyRoot = createTempDirectory("microsmith-init-detect-ruby-gemfile")
            try {
                rubyRoot.resolve("Gemfile").writeText(
                    """
                    source "https://rubygems.org"
                    gemspec
                    """.trimIndent() + "\n",
                )

                detectOnboardingProfile(rubyRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RubyOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("Gemfile"),
                    )
            } finally {
                runCatching { rubyRoot.deleteRecursively() }
            }
        }

        "detects Ruby repositories from gems.rb" {
            val rubyRoot = createTempDirectory("microsmith-init-detect-ruby-gems-rb")
            try {
                rubyRoot.resolve("gems.rb").writeText(
                    """
                    source "https://rubygems.org"
                    gem "rack"
                    """.trimIndent() + "\n",
                )

                detectOnboardingProfile(rubyRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RubyOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("gems.rb"),
                    )
            } finally {
                runCatching { rubyRoot.deleteRecursively() }
            }
        }

        "detects Ruby repositories from a root gemspec" {
            val rubyRoot = createTempDirectory("microsmith-init-detect-ruby-gemspec")
            try {
                rubyRoot.resolve("microsmith-ruby-fixture.gemspec").writeText(
                    """
                    Gem::Specification.new do |spec|
                      spec.name = "microsmith-ruby-fixture"
                      spec.version = "0.1.0"
                    end
                    """.trimIndent() + "\n",
                )

                detectOnboardingProfile(rubyRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RubyOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("microsmith-ruby-fixture.gemspec"),
                    )
            } finally {
                runCatching { rubyRoot.deleteRecursively() }
            }
        }

        "keeps the Ruby profile when both Gemfile and root gemspec are present" {
            val rubyRoot = createTempDirectory("microsmith-init-detect-ruby-multi")
            try {
                rubyRoot.resolve("Gemfile").writeText(
                    """
                    source "https://rubygems.org"
                    gemspec
                    """.trimIndent() + "\n",
                )
                rubyRoot.resolve("microsmith-ruby-fixture.gemspec").writeText(
                    """
                    Gem::Specification.new do |spec|
                      spec.name = "microsmith-ruby-fixture"
                      spec.version = "0.1.0"
                    end
                    """.trimIndent() + "\n",
                )

                detectOnboardingProfile(rubyRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RubyOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("Gemfile", "microsmith-ruby-fixture.gemspec"),
                    )
            } finally {
                runCatching { rubyRoot.deleteRecursively() }
            }
        }

        "falls back to the generic profile when a nested gemspec exists without a root Ruby marker" {
            val repoRoot = createTempDirectory("microsmith-init-detect-ruby-nested-only")
            try {
                repoRoot.resolve("vendor/ruby-fixture").createDirectories()
                repoRoot.resolve("vendor/ruby-fixture/microsmith-ruby-fixture.gemspec").writeText(
                    """
                    Gem::Specification.new do |spec|
                      spec.name = "microsmith-ruby-fixture"
                      spec.version = "0.1.0"
                    end
                    """.trimIndent() + "\n",
                )

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

        "falls back to the generic profile when Ruby and another ecosystem marker both match" {
            val mixedRoot = createTempDirectory("microsmith-init-detect-ruby-mixed")
            try {
                mixedRoot.resolve("Gemfile").writeText("source \"https://rubygems.org\"\n")
                mixedRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")

                detectOnboardingProfile(mixedRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                        matchedMarkers = listOf("Gemfile", "package.json"),
                    )
            } finally {
                runCatching { mixedRoot.deleteRecursively() }
            }
        }
    })
