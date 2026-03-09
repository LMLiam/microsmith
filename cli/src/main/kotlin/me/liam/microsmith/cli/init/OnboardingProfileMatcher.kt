package me.liam.microsmith.cli.init

import java.nio.file.Path

internal data class OnboardingProfileMatcher(
    val profile: OnboardingProfile,
    val detect: (Path) -> List<String>,
)
