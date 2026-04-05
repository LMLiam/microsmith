package io.github.lmliam.microsmith.cli.init

internal data object PythonOnboardingProfile : OnboardingProfile {
    override val id = "python"
    override val displayName = "Python"
    override val sampleMessageName = "PythonUserCreated"
    override val recommendedOutputDirectory = null
}
