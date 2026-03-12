package me.liam.microsmith.cli.init

internal interface OnboardingProfile {
    val id: OnboardingProfileId
    val displayName: String
    val sampleMessageName: String
    val recommendedOutputDirectory: String?

    val bootstrapTargetDescription: String
        get() = "$displayName repository"
}
