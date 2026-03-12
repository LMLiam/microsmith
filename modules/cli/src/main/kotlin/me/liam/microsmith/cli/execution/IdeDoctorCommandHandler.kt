package me.liam.microsmith.cli.execution

import me.liam.microsmith.cli.command.IdeDoctorCommand
import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.cli.ide.IdeDoctorResult

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

        return if (result.hasFailures) {
            emitter.error(CliFailureCode.IDE_DOCTOR_FAILED, "JetBrains IDE helper doctor detected issues.")
            CliFailureCode.IDE_DOCTOR_FAILED.exitCode
        } else {
            emitter.info("JetBrains IDE helper doctor checks passed.")
            0
        }
    }
}
