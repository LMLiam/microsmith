package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class ProcessIsolationFailureFactoryTests :
    StringSpec({
        val factory = ProcessIsolationFailureFactory()

        "returns parsed success when worker exits cleanly" {
            val success = ScriptRunSuccess(warnings = listOf("warning"), cacheHit = true, elapsedMillis = 42)

            factory.fromOutcome(
                ProcessIsolationExecutionOutcome(
                    exitCode = 0,
                    processOutput = "",
                    parsedResult = success,
                ),
            ) shouldBe success
        }

        "appends process output to parsed worker failures" {
            val failure = ScriptRunFailure(diagnostics = listOf("worker failure"), type = ScriptFailureType.COMPILATION)

            val result =
                factory.fromOutcome(
                    ProcessIsolationExecutionOutcome(
                        exitCode = 1,
                        processOutput = "stacktrace",
                        parsedResult = failure,
                    ),
                ).shouldBeTypeOf<ScriptRunFailure>()

            result.diagnostics.shouldContainExactly(
                "worker failure",
                "Process stderr/stdout: stacktrace",
            )
            result.type shouldBe ScriptFailureType.COMPILATION
        }

        "creates host failures when worker exits without a parseable result" {
            val result =
                factory.fromOutcome(
                    ProcessIsolationExecutionOutcome(
                        exitCode = 2,
                        processOutput = "could not start",
                        parsedResult = null,
                    ),
                ).shouldBeTypeOf<ScriptRunFailure>()

            result.diagnostics.shouldContainExactly(
                "Process isolation execution failed with exit code 2.",
                "Process stderr/stdout: could not start",
            )
            result.type shouldBe ScriptFailureType.HOST
        }
    })
