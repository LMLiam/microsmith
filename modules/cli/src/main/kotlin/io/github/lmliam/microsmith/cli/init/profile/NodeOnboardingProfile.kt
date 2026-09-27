package io.github.lmliam.microsmith.cli.init.profile

internal data object NodeOnboardingProfile : OnboardingProfile {
    override val id = "node"
    override val displayName = "Node"
    override val sampleMessageName = "NodeUserCreated"
    override val recommendedOutputDirectory = null
}
