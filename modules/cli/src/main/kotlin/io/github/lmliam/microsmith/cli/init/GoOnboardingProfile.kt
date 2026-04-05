package io.github.lmliam.microsmith.cli.init

internal data object GoOnboardingProfile : OnboardingProfile {
    override val id = "go"
    override val displayName = "Go"
    override val sampleMessageName = "GoUserCreated"
    override val recommendedOutputDirectory = "./internal/gen"
}
