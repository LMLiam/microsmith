package io.github.lmliam.microsmith.cli.init.profile

internal data object GenericOnboardingProfile : OnboardingProfile {
    override val id = "generic"
    override val displayName = "Other"
    override val sampleMessageName = "UserCreated"
    override val recommendedOutputDirectory = null
    override val bootstrapTargetDescription = "repository"
}
