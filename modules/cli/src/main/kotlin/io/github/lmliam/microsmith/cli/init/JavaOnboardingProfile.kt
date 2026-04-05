package io.github.lmliam.microsmith.cli.init

internal data object JavaOnboardingProfile : OnboardingProfile {
    override val id = "java"
    override val displayName = "Java"
    override val sampleMessageName = "JavaUserCreated"
    override val recommendedOutputDirectory = null
}
