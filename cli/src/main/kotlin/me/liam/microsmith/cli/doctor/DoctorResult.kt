package me.liam.microsmith.cli.doctor

internal data class DoctorResult(val checks: List<DoctorCheckResult>) {
    val hasFailures: Boolean
        get() = checks.any { it.status == DoctorCheckStatus.FAIL }
}
