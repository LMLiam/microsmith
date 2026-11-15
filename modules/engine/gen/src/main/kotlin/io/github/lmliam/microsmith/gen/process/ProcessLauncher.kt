package io.github.lmliam.microsmith.gen.process

interface ProcessLauncher {
    suspend fun run(command: List<String>): ProcessResult
}
