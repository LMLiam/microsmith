package me.liam.microsmith.sbt

class MicrosmithSbtExecutionService(
    private val requestFactory: MicrosmithSbtExecutionRequestFactory = MicrosmithSbtExecutionRequestFactory(),
    private val scriptHostRunner: MicrosmithSbtScriptHostRunner = DefaultMicrosmithSbtScriptHostRunner,
    private val resultInterpreter: MicrosmithSbtResultInterpreter = MicrosmithSbtResultInterpreter(),
) {
    fun execute(configuration: MicrosmithSbtExecutionConfiguration): MicrosmithSbtExecutionOutcome =
        runWithHostFailureMapping {
            val request = requestFactory.create(configuration)
            val result = scriptHostRunner.run(request.cacheDirectory, request.scriptRunRequest)
            resultInterpreter.interpret(request.outputDirectory, result)
        }

    @Suppress("TooGenericExceptionCaught")
    private inline fun runWithHostFailureMapping(
        action: () -> MicrosmithSbtExecutionOutcome,
    ): MicrosmithSbtExecutionOutcome {
        try {
            return action()
        } catch (error: RuntimeException) {
            throw error.toHostFailure()
        }
    }

    private fun RuntimeException.toHostFailure(): RuntimeException = when (this) {
        is MicrosmithSbtScriptFailureException -> this
        is MicrosmithSbtHostFailureException -> this
        else -> MicrosmithSbtHostFailureException(
            "Microsmith sbt plugin failed before generation completed.",
            this,
        )
    }
}
