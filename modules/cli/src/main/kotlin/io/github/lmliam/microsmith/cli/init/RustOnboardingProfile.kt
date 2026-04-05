package io.github.lmliam.microsmith.cli.init

internal data object RustOnboardingProfile : OnboardingProfile {
    override val id = "rust"
    override val displayName = "Rust"
    override val sampleMessageName = "RustUserCreated"
    override val recommendedOutputDirectory = null
}
