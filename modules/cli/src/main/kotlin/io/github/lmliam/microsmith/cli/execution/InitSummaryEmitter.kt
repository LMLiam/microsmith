package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.command.InitCommand
import io.github.lmliam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import io.github.lmliam.microsmith.cli.init.InitBootstrapResult
import io.github.lmliam.microsmith.cli.init.JavaOnboardingProfile
import io.github.lmliam.microsmith.cli.init.KotlinOnboardingProfile
import io.github.lmliam.microsmith.cli.init.ScalaOnboardingProfile

private const val GRADLE_PLUGIN_ID = "io.github.lmliam.microsmith.gradle"
private const val MICROSMITH_GROUP = "io.github.lmliam.microsmith"
private const val MICROSMITH_MAVEN_PLUGIN_ARTIFACT = "microsmith-maven-plugin"
private const val MICROSMITH_SBT_PLUGIN_ARTIFACT = "sbt-microsmith"
private const val MICROSMITH_RUNTIME_SCRIPTING_COORDINATE = "$MICROSMITH_GROUP:runtime-scripting"
private const val MICROSMITH_MAVEN_PLUGIN_COORDINATE =
    "$MICROSMITH_GROUP:$MICROSMITH_MAVEN_PLUGIN_ARTIFACT"

internal object InitSummaryEmitter {
    fun emit(emitter: CliDiagnosticEmitter, command: InitCommand, result: InitBootstrapResult) {
        emitBootstrapSummary(emitter, command, result)
        emitIdeHelperSummary(emitter, result)
        emitter.info("Next: microsmith run build.microsmith.kts --out ./generated")
        emitNativeJvmGuidance(emitter, result)
        result.repositoryDetection.profile.recommendedOutputDirectory?.let { outputDirectory ->
            emitter.info(
                "Optional repository-native output path: microsmith run build.microsmith.kts --out $outputDirectory",
            )
        }
    }

    private fun emitBootstrapSummary(
        emitter: CliDiagnosticEmitter,
        command: InitCommand,
        result: InitBootstrapResult,
    ) {
        if (result.createdFiles.isNotEmpty()) {
            emitter.info("Created bootstrap files: ${result.createdFiles.formatForDisplay(result.projectRoot)}")
        }
        if (result.overwrittenFiles.isNotEmpty()) {
            emitter.info("Overwrote bootstrap files: ${result.overwrittenFiles.formatForDisplay(result.projectRoot)}")
        }
        if (result.preservedFiles.isNotEmpty()) {
            val message =
                if (command.force) {
                    "Preserved bootstrap files already matching the managed templates"
                } else {
                    "Preserved existing bootstrap files"
                }
            emitter.info("$message: ${result.preservedFiles.formatForDisplay(result.projectRoot)}")
            if (!command.force) {
                emitter.info(
                    "Re-run with --force to replace existing regular bootstrap files with the managed templates.",
                )
            }
        }
        if (
            result.createdFiles.isEmpty() &&
            result.overwrittenFiles.isEmpty() &&
            result.preservedFiles.isEmpty()
        ) {
            emitter.info("Bootstrap completed with no managed file changes.")
        }
    }

    private fun emitIdeHelperSummary(emitter: CliDiagnosticEmitter, result: InitBootstrapResult) {
        val ideHelperResult = result.ideHelperResult
        if (ideHelperResult == null) {
            emitter.info(
                "JetBrains IDE helper generation was skipped. Run 'microsmith ide refresh' when you want IDE indexing.",
            )
            return
        }

        val helperRoot = ideHelperResult.helperRoot.toAbsolutePath().normalize()
        val state = if (ideHelperResult.updatedFiles.isEmpty()) "already current" else "updated"
        emitter.info("JetBrains IDE helper is $state at '$helperRoot'.")
        emitter.info("Import '${helperRoot.resolve("build.gradle.kts")}' as a Gradle project in JetBrains IDEs.")
    }

    private fun emitNativeJvmGuidance(emitter: CliDiagnosticEmitter, result: InitBootstrapResult) {
        val nativeBuildSystem = result.nativeJvmBuildSystem() ?: return
        when (nativeBuildSystem) {
            JvmNativeBuildSystem.GRADLE -> emitter.info(
                "Gradle repository detected. Prefer the native Gradle plugin path when you want imported-project " +
                    "IDE support: apply plugin id '$GRADLE_PLUGIN_ID', configure 'microsmith { ... }', " +
                    "and run './gradlew microsmithGenerate'.",
            )
            JvmNativeBuildSystem.MAVEN -> emitter.info(
                "Maven repository detected. Prefer the native Maven plugin path when you want imported-project " +
                    "IDE support: add '$MICROSMITH_RUNTIME_SCRIPTING_COORDINATE' as a provided dependency, " +
                    "configure '$MICROSMITH_MAVEN_PLUGIN_COORDINATE', and run 'mvn microsmith:generate'.",
            )
            JvmNativeBuildSystem.SBT -> emitter.info(
                "sbt repository detected. Prefer the native sbt plugin path when you want build-aligned " +
                    "generation: add '$MICROSMITH_GROUP' % '$MICROSMITH_SBT_PLUGIN_ARTIFACT' in project/plugins.sbt, " +
                    "enable 'MicrosmithSbtPlugin', and run 'sbt microsmithGenerate'.",
            )
        }
    }
}

private fun InitBootstrapResult.nativeJvmBuildSystem(): JvmNativeBuildSystem? {
    val profile = repositoryDetection.profile
    if (profile != JavaOnboardingProfile && profile != KotlinOnboardingProfile && profile != ScalaOnboardingProfile) {
        return null
    }
    val hasGradleMarker = repositoryDetection.matchedMarkers.any(::isGradleMarker)
    val hasMavenMarker = repositoryDetection.matchedMarkers.any(::isMavenMarker)
    val hasSbtMarker = repositoryDetection.matchedMarkers.any(::isSbtMarker)
    return when {
        hasGradleMarker && !hasMavenMarker && !hasSbtMarker -> JvmNativeBuildSystem.GRADLE
        hasMavenMarker && !hasGradleMarker && !hasSbtMarker -> JvmNativeBuildSystem.MAVEN
        hasSbtMarker && !hasGradleMarker && !hasMavenMarker -> JvmNativeBuildSystem.SBT
        else -> null
    }
}

private fun isGradleMarker(marker: String): Boolean = marker in setOf(
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
)

private fun isMavenMarker(marker: String): Boolean = marker == "pom.xml"

private fun isSbtMarker(marker: String): Boolean = marker in setOf(
    "build.sbt",
    "project/build.properties",
)

private enum class JvmNativeBuildSystem {
    GRADLE,
    MAVEN,
    SBT,
}
