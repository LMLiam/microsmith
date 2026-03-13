package io.github.lmliam.microsmith.cli.init

internal data object KotlinOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = OnboardingProfileId("kotlin")
    override val displayName: String = "Kotlin"
    override val sampleMessageName: String = "KotlinUserCreated"
    override val recommendedOutputDirectory: String? = null
}
