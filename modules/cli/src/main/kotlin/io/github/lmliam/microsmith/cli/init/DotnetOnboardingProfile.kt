package io.github.lmliam.microsmith.cli.init

internal data object DotnetOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("dotnet")
    override val displayName: String = ".NET"
    override val sampleMessageName: String = "DotnetUserCreated"
    override val recommendedOutputDirectory: String = "./Generated"
}
