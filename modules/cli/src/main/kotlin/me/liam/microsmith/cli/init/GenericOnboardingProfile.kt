package me.liam.microsmith.cli.init

internal data object GenericOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("generic")
    override val displayName: String = "Other"
    override val sampleMessageName: String = "UserCreated"
    override val recommendedOutputDirectory: String? = null
    override val bootstrapTargetDescription: String = "repository"
}
