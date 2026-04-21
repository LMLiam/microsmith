package io.github.lmliam.microsmith.cli.init

internal object BootstrapScriptTemplates {
    fun filesFor(repositoryDetection: OnboardingProfileDetection): Map<String, String> = linkedMapOf(
        "settings.microsmith.kts" to renderDefaultSettingsScript(repositoryDetection),
        "build.microsmith.kts" to renderDefaultBuildScript(repositoryDetection.profile),
    )

    private fun renderDefaultSettingsScript(repositoryDetection: OnboardingProfileDetection): String = buildString {
        appendLine("// Microsmith repository settings.")
        appendLine("// ${repositoryDetection.describeForComment()}.")
        appendLine("// Add shared script configuration here as your repository grows.")
    }

    private fun renderDefaultBuildScript(profile: OnboardingProfile): String = buildString {
        if (profile == DotnetOnboardingProfile) {
            append(DotnetBootstrapScriptTemplateRenderer.render(profile))
            return@buildString
        }
        appendLine("// Bootstrapped Microsmith schema for this ${profile.bootstrapTargetDescription}.")
        appendLine("// Canonical first run:")
        appendLine("// microsmith run build.microsmith.kts")
        appendLine("// Generated .proto files land in ./proto by default.")
        profile.recommendedOutputDirectory?.let { outputDirectory ->
            appendLine("// Common repository-native output path:")
            appendLine("// microsmith run build.microsmith.kts --out $outputDirectory")
        }
        appendLine(SchemaBootstrapScriptTemplateRenderer.render(profile))
    }
}
