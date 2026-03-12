package me.liam.microsmith.cli.init

import java.nio.file.Path

internal class OnboardingProfileDetector(
    private val matchers: List<OnboardingProfileMatcher> = BuiltInOnboardingProfileMatchers.all(),
    private val fallbackProfile: OnboardingProfile = GenericOnboardingProfile,
) {
    init {
        OnboardingProfileConflictValidator.requireConsistentDefinitions(matchers, fallbackProfile)
    }

    fun detect(projectRoot: Path): OnboardingProfileDetection {
        val matches =
            matchers.mapNotNull { matcher ->
                val matchedMarkers = matcher.detect(projectRoot).sorted()
                matchedMarkers
                    .takeIf(List<String>::isNotEmpty)
                    ?.let {
                        OnboardingProfileMatch(
                            profile = matcher.profile,
                            matchedMarkers = it,
                        )
                    }
            }

        val matchedProfiles = matches.map(OnboardingProfileMatch::profile).distinctBy(OnboardingProfile::id)
        val matchedMarkers = matches.flatMap(OnboardingProfileMatch::matchedMarkers).distinct().sorted()
        if (matchedProfiles.isEmpty()) {
            return OnboardingProfileDetection(
                profile = fallbackProfile,
                selectionReason = OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                matchedMarkers = matchedMarkers,
            )
        }
        if (matchedProfiles.size == 1) {
            return OnboardingProfileDetection(
                profile = matchedProfiles.single(),
                selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                matchedMarkers = matchedMarkers,
            )
        }

        return OnboardingProfileDetection(
            profile = fallbackProfile,
            selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
            matchedMarkers = matchedMarkers,
        )
    }
}

internal fun detectOnboardingProfile(
    projectRoot: Path,
    detector: OnboardingProfileDetector = OnboardingProfileDetector(),
): OnboardingProfileDetection = detector.detect(projectRoot)
