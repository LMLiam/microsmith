package me.liam.microsmith.cli.init

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path

class OnboardingProfileDetectorTests :
    StringSpec({
        "keeps a specific profile when multiple matchers map to the same profile" {
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("pnpm-workspace.yaml") },
                    ),
                )

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = NodeOnboardingProfile,
                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                    matchedMarkers = listOf("package.json", "pnpm-workspace.yaml"),
                )
        }

        "deduplicates repeated markers from overlapping matchers" {
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                    ),
                )

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = NodeOnboardingProfile,
                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                    matchedMarkers = listOf("package.json"),
                )
        }

        "falls back to the generic profile when multiple distinct profiles match" {
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(GoOnboardingProfile) { listOf("go.mod") },
                    ),
                )

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = GenericOnboardingProfile,
                    selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                    matchedMarkers = listOf("go.mod", "package.json"),
                )
        }

        "uses the configured fallback profile when no markers match" {
            val customFallback =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("custom-fallback")
                    override val displayName: String = "Custom"
                    override val sampleMessageName: String = "CustomUserCreated"
                    override val recommendedOutputDirectory: String? = null
                }
            val detector = OnboardingProfileDetector(matchers = emptyList(), fallbackProfile = customFallback)

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = customFallback,
                    selectionReason = OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                    matchedMarkers = emptyList(),
                )
        }

        "formatting uses selection reason rather than generic profile identity" {
            val customFallback =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("custom-fallback")
                    override val displayName: String = "Custom"
                    override val sampleMessageName: String = "CustomUserCreated"
                    override val recommendedOutputDirectory: String? = null
                }
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(GoOnboardingProfile) { listOf("go.mod") },
                    ),
                    fallbackProfile = customFallback,
                )

            val detection = detector.detect(UNUSED_PROJECT_ROOT)

            detection shouldBe
                OnboardingProfileDetection(
                    profile = customFallback,
                    selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                    matchedMarkers = listOf("go.mod", "package.json"),
                )
            detection.describeForComment().shouldContain("multiple repository markers matched")
            detection.describeForSummary().shouldContain("multiple markers matched")
        }

        "rejects conflicting profile definitions that reuse the same stable id" {
            val primaryProfile =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python"
                    override val sampleMessageName: String = "PythonUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }
            val conflictingProfile =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python Variant"
                    override val sampleMessageName: String = "PythonVariantUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }

            val error =
                io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                    OnboardingProfileDetector(
                        matchers =
                        listOf(
                            OnboardingProfileMatcher(primaryProfile) { listOf("pyproject.toml") },
                            OnboardingProfileMatcher(conflictingProfile) { listOf("requirements.txt") },
                        ),
                    )
                }

            error.message.shouldContain("python")
        }

        "rejects fallback profile definitions that reuse a matcher stable id" {
            val primaryProfile =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python"
                    override val sampleMessageName: String = "PythonUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }
            val conflictingFallback =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python Fallback"
                    override val sampleMessageName: String = "PythonFallbackUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }

            val error =
                io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                    OnboardingProfileDetector(
                        matchers = listOf(OnboardingProfileMatcher(primaryProfile) { listOf("pyproject.toml") }),
                        fallbackProfile = conflictingFallback,
                    )
                }

            error.message.shouldContain("python")
        }
    })

private val UNUSED_PROJECT_ROOT: Path = Path.of(".")
