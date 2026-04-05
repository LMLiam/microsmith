package io.github.lmliam.microsmith.cli.init

internal data object PythonOnboardingProfile : OnboardingProfile {
    override val id: OnboardingProfileId = "python"
    override val displayName: String = "Python"
    override val sampleMessageName: String = "PythonUserCreated"
    override val recommendedOutputDirectory: String? = null
}
