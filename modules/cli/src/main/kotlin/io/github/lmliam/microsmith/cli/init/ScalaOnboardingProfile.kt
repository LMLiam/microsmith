package io.github.lmliam.microsmith.cli.init

internal data object ScalaOnboardingProfile : OnboardingProfile {
    override val id = "scala"
    override val displayName = "Scala"
    override val sampleMessageName = "ScalaUserCreated"
    override val recommendedOutputDirectory = null
}
