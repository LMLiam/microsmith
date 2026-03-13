package io.github.lmliam.microsmith.cli.init

internal data class OnboardingProfileDetection(
    val profile: OnboardingProfile,
    val selectionReason: OnboardingProfileSelectionReason,
    val matchedMarkers: List<String>,
)

internal fun OnboardingProfileDetection.describeForComment(): String = buildString {
    append("Detected repository profile: ${profile.displayName}")
    when (selectionReason) {
        OnboardingProfileSelectionReason.NO_MARKERS_MATCHED -> append(" (no repo markers matched)")
        OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS ->
            append(" (multiple repository markers matched: ${matchedMarkers.joinToString(separator = ", ")})")
        OnboardingProfileSelectionReason.MATCHED_PROFILE ->
            append(" via ${matchedMarkers.joinToString(separator = ", ")}")
    }
}

internal fun OnboardingProfileDetection.describeForSummary(): String = buildString {
    append(profile.displayName)
    when (selectionReason) {
        OnboardingProfileSelectionReason.NO_MARKERS_MATCHED -> Unit
        OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS ->
            append(" (multiple markers matched: ${matchedMarkers.joinToString(separator = ", ")})")
        OnboardingProfileSelectionReason.MATCHED_PROFILE ->
            append(" (matched ${matchedMarkers.joinToString(separator = ", ")})")
    }
}
