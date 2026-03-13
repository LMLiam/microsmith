package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.command.InitCommand
import io.github.lmliam.microsmith.cli.diagnostics.CliFailureCode
import io.github.lmliam.microsmith.cli.init.InitBootstrapResult
import io.github.lmliam.microsmith.cli.init.InitConflictException
import io.github.lmliam.microsmith.cli.init.InitValidationException
import io.github.lmliam.microsmith.cli.init.describeForSummary

internal class InitCommandHandler(
    private val emitterFactory: CliDiagnosticEmitterFactory,
    private val initRunner: (InitCommand) -> InitBootstrapResult,
) {
    fun execute(command: InitCommand): Int {
        val emitter = emitterFactory.create(command.diagnosticsFormat, command.verbose)
        val result =
            runCatching {
                initRunner(command)
            }.getOrElse { error ->
                val code =
                    when (error) {
                        is InitConflictException -> CliFailureCode.INIT_CONFLICT
                        is InitValidationException -> CliFailureCode.INIT_VALIDATION_FAILED
                        else -> CliFailureCode.INIT_RUNTIME_FAILED
                    }
                emitter.error(code, error.message ?: "Microsmith init failed.")
                return code.exitCode
            }

        val projectRoot = result.projectRoot.toAbsolutePath().normalize()
        emitter.info(
            "Microsmith init completed at '$projectRoot'.",
            details =
            mapOf(
                "projectRoot" to projectRoot.toString(),
                "repositoryProfile" to result.repositoryDetection.profile.id.toString(),
                "repositoryProfileDisplayName" to result.repositoryDetection.profile.displayName,
                "matchedMarkers" to result.repositoryDetection.matchedMarkers.joinToString(separator = ","),
                "createdFiles" to result.createdFiles.size.toString(),
                "overwrittenFiles" to result.overwrittenFiles.size.toString(),
                "preservedFiles" to result.preservedFiles.size.toString(),
                "ideHelperUpdatedFiles" to (result.ideHelperResult?.updatedFiles?.size ?: 0).toString(),
                "ideHelperSkipped" to command.skipIdeHelper.toString(),
                "force" to command.force.toString(),
            ),
        )
        emitter.info("Detected repository profile: ${result.repositoryDetection.describeForSummary()}.")
        InitSummaryEmitter.emit(emitter = emitter, command = command, result = result)
        return 0
    }
}
