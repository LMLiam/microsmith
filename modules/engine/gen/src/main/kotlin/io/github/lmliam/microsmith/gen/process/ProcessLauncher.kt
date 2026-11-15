package io.github.lmliam.microsmith.gen.process

interface ProcessLauncher {
    suspend fun run(command: List<String>): ProcessResult
}

class ProcessResult(val exitCode: Int, val output: String)
