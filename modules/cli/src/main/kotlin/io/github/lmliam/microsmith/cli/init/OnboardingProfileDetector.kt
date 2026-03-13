package io.github.lmliam.microsmith.cli.init

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
        val (profile, selectionReason) =
            when {
                matchedProfiles.isEmpty() ->
                    fallbackProfile to OnboardingProfileSelectionReason.NO_MARKERS_MATCHED

                matchedProfiles.size == 1 ->
                    matchedProfiles.single() to OnboardingProfileSelectionReason.MATCHED_PROFILE

                else ->
                    fallbackProfile to OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS
            }

        return OnboardingProfileDetection(
            profile = profile,
            selectionReason = selectionReason,
            matchedMarkers = matchedMarkers,
        )
    }
}

internal fun detectOnboardingProfile(
    projectRoot: Path,
    detector: OnboardingProfileDetector = OnboardingProfileDetector(),
): OnboardingProfileDetection = detector.detect(projectRoot)
