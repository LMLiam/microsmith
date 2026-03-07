package me.liam.microsmith.cli.init

internal object BootstrapScriptTemplates {
    fun filesFor(repositoryDetection: OnboardingRepositoryDetection): Map<String, String> = linkedMapOf(
        "settings.microsmith.kts" to renderDefaultSettingsScript(repositoryDetection),
        "build.microsmith.kts" to renderDefaultBuildScript(repositoryDetection.type),
    )

    private fun renderDefaultSettingsScript(repositoryDetection: OnboardingRepositoryDetection): String = buildString {
        appendLine("// Microsmith repository settings.")
        appendLine("// ${repositoryDetection.describeForComment()}.")
        appendLine("// Add shared script configuration here as your repository grows.")
    }

    private fun renderDefaultBuildScript(repositoryType: OnboardingRepositoryType): String = buildString {
        val displayName =
            if (repositoryType == OnboardingRepositoryType.OTHER) {
                "repository"
            } else {
                "${repositoryType.displayName} repository"
            }
        appendLine("// Bootstrapped Microsmith schema for this $displayName.")
        appendLine("// Canonical first run:")
        appendLine("// microsmith run build.microsmith.kts --out ./generated")
        repositoryType.repoNativeOutputDirectory?.let { outputDirectory ->
            appendLine("// Common repository-native output path:")
            appendLine("// microsmith run build.microsmith.kts --out $outputDirectory")
        }
        appendLine(
            """
            microsmith {
                schemas {
                    protobuf {
                        message("${repositoryType.sampleMessageName}") {
                            int32("id") { index(1) }
                            string("email") { index(2) }
                        }
                    }
                }
            }
            """.trimIndent(),
        )
    }
}
