package io.github.lmliam.microsmith.cli.init

internal data object NodeOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("node")
    override val displayName: String = "Node"
    override val sampleMessageName: String = "NodeUserCreated"
    override val recommendedOutputDirectory: String = "./generated"
}
