package io.github.lmliam.microsmith.gradle.worker

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunDiagnostic
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MicrosmithGradleWorkerResultMapperTests :
    StringSpec(
        {
            "maps a resolution failure to worker diagnostics that keep their issue codes" {
                val failure =
                    ScriptRunFailure(
                        diagnostics =
                            listOf(
                                ScriptRunDiagnostic(message = "first problem", code = "protobuf.first"),
                                ScriptRunDiagnostic(message = "uncoded"),
                            ),
                        type = ScriptFailureType.RESOLUTION,
                    )

                failure.toWorkerResult() shouldBe
                    MicrosmithGradleWorkerFailure(
                        diagnostics =
                            listOf(
                                MicrosmithGradleWorkerDiagnostic(message = "first problem", code = "protobuf.first"),
                                MicrosmithGradleWorkerDiagnostic(message = "uncoded"),
                            ),
                        type = "RESOLUTION",
                    )
            }
        },
    )
