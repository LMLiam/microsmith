package me.liam.microsmith.gradle

internal data class MicrosmithGradleWorkerFailure(
    val diagnostics: List<String>,
    val type: String,
) : MicrosmithGradleWorkerResult
