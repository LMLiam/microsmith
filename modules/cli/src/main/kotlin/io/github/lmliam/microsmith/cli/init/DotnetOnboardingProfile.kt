package io.github.lmliam.microsmith.cli.init

internal data object DotnetOnboardingProfile : OnboardingProfile {
    override val id = "dotnet"
    override val displayName = ".NET"
    override val sampleMessageName = "DotnetUserCreated"
    override val recommendedOutputDirectory = "./Generated"
}
