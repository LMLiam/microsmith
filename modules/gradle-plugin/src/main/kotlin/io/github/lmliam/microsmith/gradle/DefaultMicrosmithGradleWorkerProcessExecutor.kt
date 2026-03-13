package io.github.lmliam.microsmith.gradle

internal class DefaultMicrosmithGradleWorkerProcessExecutor : MicrosmithGradleWorkerProcessExecutor {
    override fun execute(command: List<String>): MicrosmithGradleWorkerProcessOutcome {
        val process =
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
        val processOutput = process.inputStream.bufferedReader().use { reader -> reader.readText().trim() }
        val exitCode = process.waitFor()
        return MicrosmithGradleWorkerProcessOutcome(exitCode, processOutput)
    }
}
