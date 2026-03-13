package io.github.lmliam.microsmith.cli.init

internal data object RustOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("rust")
    override val displayName: String = "Rust"
    override val sampleMessageName: String = "RustUserCreated"
    override val recommendedOutputDirectory: String? = null
}
