package me.liam.microsmith.cli.init

import java.nio.file.Path

internal class OnboardingProfileDetector(
    private val matchers: List<OnboardingProfileMatcher> = BuiltInOnboardingProfileMatchers.all(),
    private val fallbackProfile: OnboardingProfile = GenericOnboardingProfile,
) {
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
        val resolvedProfile =
            when (matchedProfiles.size) {
                1 -> matchedProfiles.single()
                else -> fallbackProfile
            }

        return OnboardingProfileDetection(
            profile = resolvedProfile,
            matchedMarkers = matches.flatMap { (_, matchedMarkers) -> matchedMarkers }.distinct().sorted(),
        )
    }
}

internal fun detectOnboardingProfile(
    projectRoot: Path,
    detector: OnboardingProfileDetector = OnboardingProfileDetector(),
): OnboardingProfileDetection = detector.detect(projectRoot)
