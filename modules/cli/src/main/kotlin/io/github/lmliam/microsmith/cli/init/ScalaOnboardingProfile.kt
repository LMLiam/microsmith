package io.github.lmliam.microsmith.cli.init

internal data object ScalaOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = "scala"
    override val displayName: String = "Scala"
    override val sampleMessageName: String = "ScalaUserCreated"
    override val recommendedOutputDirectory: String? = null
}
