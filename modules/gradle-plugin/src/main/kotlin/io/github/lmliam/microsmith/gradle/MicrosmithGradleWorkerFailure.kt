package io.github.lmliam.microsmith.gradle

internal data class MicrosmithGradleWorkerFailure(val diagnostics: List<String>, val type: String) :
    MicrosmithGradleWorkerResult
