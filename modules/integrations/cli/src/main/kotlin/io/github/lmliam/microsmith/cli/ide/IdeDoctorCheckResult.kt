package io.github.lmliam.microsmith.cli.ide

internal data class IdeDoctorCheckResult(
    val id: String,
    val passed: Boolean,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)
