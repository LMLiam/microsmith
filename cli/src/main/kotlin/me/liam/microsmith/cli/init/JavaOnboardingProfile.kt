package me.liam.microsmith.cli.init

internal data object JavaOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("java")
    override val displayName: String = "Java"
    override val sampleMessageName: String = "JavaUserCreated"
    override val recommendedOutputDirectory: String? = null
}
