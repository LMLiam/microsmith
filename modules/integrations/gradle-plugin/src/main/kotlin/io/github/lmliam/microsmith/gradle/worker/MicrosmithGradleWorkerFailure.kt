package io.github.lmliam.microsmith.gradle.worker

internal data class MicrosmithGradleWorkerFailure(val diagnostics: List<String>, val type: String) :
    MicrosmithGradleWorkerResult
