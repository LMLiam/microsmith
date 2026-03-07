package me.liam.microsmith.cli.init

internal data class OnboardingRepositoryDetection(
    val type: OnboardingRepositoryType,
    val matchedMarkers: List<String>,
)

internal fun OnboardingRepositoryDetection.describeForComment(): String = buildString {
    append("Detected repository type: ${type.displayName}")
    if (matchedMarkers.isEmpty()) {
        append(" (no repo markers matched)")
    } else {
        append(" via ${matchedMarkers.joinToString(separator = ", ")}")
    }
}

internal fun OnboardingRepositoryDetection.describeForSummary(): String = buildString {
    append(type.displayName)
    if (matchedMarkers.isNotEmpty()) {
        append(" (matched ${matchedMarkers.joinToString(separator = ", ")})")
    }
}
