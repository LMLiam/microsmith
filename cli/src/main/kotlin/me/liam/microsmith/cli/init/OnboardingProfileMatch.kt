package me.liam.microsmith.cli.init

internal data class OnboardingProfileMatch(
    val profile: OnboardingProfile,
    val matchedMarkers: List<String>,
)
