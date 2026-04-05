package io.github.lmliam.microsmith.cli.init

internal object OnboardingProfileConflictValidator {
    fun requireConsistentDefinitions(matchers: List<OnboardingProfileMatcher>, fallbackProfile: OnboardingProfile) {
        val conflictingIds = conflictingProfileIds(matchers, fallbackProfile)
        require(conflictingIds.isEmpty()) {
            "Onboarding profile ids must map to exactly one profile definition: " +
                conflictingIds.joinToString(separator = ", ")
        }
    }

    private fun conflictingProfileIds(
        matchers: List<OnboardingProfileMatcher>,
        fallbackProfile: OnboardingProfile,
    ): List<OnboardingProfileId> {
        return matchers
            .map(OnboardingProfileMatcher::profile)
            .plus(fallbackProfile)
            .groupBy(OnboardingProfile::id)
            .filterValues { groupedProfiles -> groupedProfiles.distinct().size > 1 }
            .keys
            .sorted()
    }
}
