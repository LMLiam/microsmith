package me.liam.microsmith.cli.execution

import me.liam.microsmith.cli.command.DoctorCommand
import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.cli.doctor.DoctorCheckStatus
import me.liam.microsmith.cli.doctor.DoctorResult

internal class DoctorCommandHandler(
    private val emitterFactory: CliDiagnosticEmitterFactory,
    private val doctorRunner: () -> DoctorResult,
) {
    fun execute(command: DoctorCommand): Int {
        val emitter = emitterFactory.create(command.diagnosticsFormat, command.verbose)
        val result = doctorRunner()
        result.checks.forEach { check ->
            val details = mapOf("check" to check.id) + check.details
            if (check.status == DoctorCheckStatus.PASS) {
                emitter.info("doctor/${check.id}: ${check.message}", details)
                return@forEach
            }
            emitter.error(CliFailureCode.DOCTOR_FAILED, "doctor/${check.id}: ${check.message}", details)
        }

        if (!result.hasFailures) {
            emitter.info("Doctor checks passed.")
            return 0
        }
        emitter.error(CliFailureCode.DOCTOR_FAILED, "Doctor detected environment issues.")
        return CliFailureCode.DOCTOR_FAILED.exitCode
    }
}
