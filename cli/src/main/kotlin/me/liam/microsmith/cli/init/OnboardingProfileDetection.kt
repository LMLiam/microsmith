package me.liam.microsmith.cli.init

internal data class OnboardingProfileDetection(
    val profile: OnboardingProfile,
    val matchedMarkers: List<String>,
)

internal fun OnboardingProfileDetection.describeForComment(): String = buildString {
    append("Detected repository type: ${profile.displayName}")
    when {
        matchedMarkers.isEmpty() -> append(" (no repo markers matched)")
        profile == GenericOnboardingProfile ->
            append(" (multiple repository markers matched: ${matchedMarkers.joinToString(separator = ", ")})")

        else -> append(" via ${matchedMarkers.joinToString(separator = ", ")}")
    }
}

internal fun OnboardingProfileDetection.describeForSummary(): String = buildString {
    append(profile.displayName)
    when {
        matchedMarkers.isEmpty() -> Unit
        profile == GenericOnboardingProfile ->
            append(" (multiple markers matched: ${matchedMarkers.joinToString(separator = ", ")})")

        else -> append(" (matched ${matchedMarkers.joinToString(separator = ", ")})")
    }
}
