package me.liam.microsmith.cli.doctor

internal data class DoctorCheckResult(
    val id: String,
    val status: DoctorCheckStatus,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)
