package io.github.lmliam.microsmith.cli.init

import java.nio.file.Path

internal data class OnboardingProfileMatcher(
    val profile: OnboardingProfile,
    val detect: (Path) -> List<String>,
)
