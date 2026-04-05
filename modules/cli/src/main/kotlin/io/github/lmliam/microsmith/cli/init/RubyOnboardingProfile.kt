package io.github.lmliam.microsmith.cli.init

internal data object RubyOnboardingProfile : OnboardingProfile {
    override val id = "ruby"
    override val displayName = "Ruby"
    override val sampleMessageName = "RubyUserCreated"
    override val recommendedOutputDirectory = null
}
