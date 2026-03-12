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
                if (matchedMarkers.isEmpty()) {
                    null
                } else {
                    OnboardingProfileMatch(
                        profile = matcher.profile,
                        matchedMarkers = matchedMarkers,
                    )
                }
            }

        val matchedProfiles = matches.map(OnboardingProfileMatch::profile).distinctBy(OnboardingProfile::id)
        val selectionReason =
            when (matchedProfiles.size) {
                0 -> OnboardingProfileSelectionReason.NO_MARKERS_MATCHED
                1 -> OnboardingProfileSelectionReason.MATCHED_PROFILE
                else -> OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS
            }
        val resolvedProfile =
            when (selectionReason) {
                OnboardingProfileSelectionReason.MATCHED_PROFILE -> matchedProfiles.single()
                OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                -> fallbackProfile
            }

        return OnboardingProfileDetection(
            profile = resolvedProfile,
            selectionReason = selectionReason,
            matchedMarkers = matches.flatMap(OnboardingProfileMatch::matchedMarkers).distinct().sorted(),
        )
    }
}

internal fun detectOnboardingProfile(
    projectRoot: Path,
    detector: OnboardingProfileDetector = OnboardingProfileDetector(),
): OnboardingProfileDetection = detector.detect(projectRoot)
