package io.github.lmliam.microsmith.cli.init

internal data object RubyOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("ruby")
    override val displayName: String = "Ruby"
    override val sampleMessageName: String = "RubyUserCreated"
    override val recommendedOutputDirectory: String? = null
}
