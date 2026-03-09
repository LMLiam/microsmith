package me.liam.microsmith.cli.init

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
                    matchedMarkers = listOf("go.mod", "package.json"),
                )
        }
    })

private val UNUSED_PROJECT_ROOT: Path = Path.of(".")
