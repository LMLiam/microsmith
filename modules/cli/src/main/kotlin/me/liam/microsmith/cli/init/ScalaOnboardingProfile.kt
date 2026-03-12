package me.liam.microsmith.cli.init

internal data object ScalaOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("scala")
    override val displayName: String = "Scala"
    override val sampleMessageName: String = "ScalaUserCreated"
    override val recommendedOutputDirectory: String? = null
}
