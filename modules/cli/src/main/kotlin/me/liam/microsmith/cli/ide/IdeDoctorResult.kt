package me.liam.microsmith.cli.ide

import java.nio.file.Path

internal data class IdeDoctorResult(
    val projectRoot: Path,
    val helperRoot: Path,
    val checks: List<IdeDoctorCheckResult>,
) {
    val hasFailures: Boolean = checks.any { check -> !check.passed }
}
