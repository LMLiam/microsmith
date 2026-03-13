package me.liam.microsmith.gradle

internal data class MicrosmithGradleWorkerSuccess(
    val warnings: List<String>,
    val cacheHit: Boolean,
    val elapsedMillis: Long,
) : MicrosmithGradleWorkerResult
