package io.github.lmliam.microsmith.cli.init

internal data object KotlinOnboardingProfile : OnboardingProfile {
    override val id = "kotlin"
    override val displayName = "Kotlin"
    override val sampleMessageName = "KotlinUserCreated"
    override val recommendedOutputDirectory = null
}
