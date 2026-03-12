package me.liam.microsmith.cli.init

internal data object GoOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("go")
    override val displayName: String = "Go"
    override val sampleMessageName: String = "GoUserCreated"
    override val recommendedOutputDirectory: String = "./internal/gen"
}
