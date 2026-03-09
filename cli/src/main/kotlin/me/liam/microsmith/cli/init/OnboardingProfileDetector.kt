package me.liam.microsmith.cli.init

import java.nio.file.Path

internal class OnboardingProfileDetector(
    private val matchers: List<OnboardingProfileMatcher> = BuiltInOnboardingProfileMatchers.all(),
    private val fallbackProfile: OnboardingProfile = GenericOnboardingProfile,
) {
    init {
        require(conflictingProfileIds(matchers).isEmpty()) {
            val conflictingIds = conflictingProfileIds(matchers).joinToString(separator = ", ")
            "Onboarding profile ids must map to exactly one profile definition: $conflictingIds"
        }
    }

    fun detect(projectRoot: Path): OnboardingProfileDetection {
        val matches =
            matchers.mapNotNull { matcher ->
                val matchedMarkers = matcher.detect(projectRoot).sorted()
                if (matchedMarkers.isEmpty()) {
                    null
                } else {
                    matcher.profile to matchedMarkers
                }
            }

        val matchedProfiles = matches.map { (profile, _) -> profile }.distinctBy(OnboardingProfile::id)
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
            matchedMarkers = matches.flatMap { (_, matchedMarkers) -> matchedMarkers }.distinct().sorted(),
        )
    }
}

internal fun detectOnboardingProfile(
    projectRoot: Path,
    detector: OnboardingProfileDetector = OnboardingProfileDetector(),
): OnboardingProfileDetection = detector.detect(projectRoot)

private fun conflictingProfileIds(matchers: List<OnboardingProfileMatcher>): List<OnboardingProfileId> {
    return matchers
        .groupBy { it.profile.id }
        .filterValues { groupedMatchers ->
            groupedMatchers.map(OnboardingProfileMatcher::profile).distinct().size > 1
        }
        .keys
        .sortedBy(OnboardingProfileId::value)
}
