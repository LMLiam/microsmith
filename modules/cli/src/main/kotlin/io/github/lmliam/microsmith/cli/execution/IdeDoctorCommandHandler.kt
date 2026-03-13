package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.command.IdeDoctorCommand
import io.github.lmliam.microsmith.cli.diagnostics.CliFailureCode
import io.github.lmliam.microsmith.cli.ide.IdeDoctorResult

internal class IdeDoctorCommandHandler(
    private val emitterFactory: CliDiagnosticEmitterFactory,
    private val ideDoctorRunner: (IdeDoctorCommand) -> IdeDoctorResult,
) {
    fun execute(command: IdeDoctorCommand): Int {
        val emitter = emitterFactory.create(command.diagnosticsFormat, command.verbose)
        val result =
            runCatching {
                ideDoctorRunner(command)
            }.getOrElse { error ->
                emitter.error(
                    CliFailureCode.IDE_DOCTOR_FAILED,
                    error.message ?: "JetBrains IDE helper doctor failed unexpectedly.",
                )
                return CliFailureCode.IDE_DOCTOR_FAILED.exitCode
            }

        result.checks.forEach { check ->
            if (check.passed) {
                emitter.info("ide-doctor/${check.id}: ${check.message}", check.details)
            } else {
                emitter.error(
                    CliFailureCode.IDE_DOCTOR_FAILED,
                    "ide-doctor/${check.id}: ${check.message}",
                    check.details,
                )
            }
        }

        if (!result.hasFailures) {
            emitter.info("JetBrains IDE helper doctor checks passed.")
            return 0
        }
        emitter.error(CliFailureCode.IDE_DOCTOR_FAILED, "JetBrains IDE helper doctor detected issues.")
        return CliFailureCode.IDE_DOCTOR_FAILED.exitCode
    }
}
