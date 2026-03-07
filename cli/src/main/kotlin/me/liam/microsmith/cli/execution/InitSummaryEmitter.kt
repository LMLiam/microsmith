package me.liam.microsmith.cli.execution

import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import me.liam.microsmith.cli.init.InitBootstrapResult

internal object InitSummaryEmitter {
    fun emit(emitter: CliDiagnosticEmitter, command: InitCommand, result: InitBootstrapResult) {
        emitBootstrapSummary(emitter, command, result)
        emitIdeHelperSummary(emitter, result)
        emitter.info("Next: microsmith run build.microsmith.kts --out ./generated")
        result.repositoryDetection.type.repoNativeOutputDirectory?.let { outputDirectory ->
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
}
