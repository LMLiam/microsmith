package io.github.lmliam.microsmith.gen.process

internal interface ProcessLauncher {
    suspend fun run(command: List<String>): ProcessResult
}
