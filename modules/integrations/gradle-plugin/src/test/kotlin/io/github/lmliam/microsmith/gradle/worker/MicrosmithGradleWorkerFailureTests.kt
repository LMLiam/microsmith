package io.github.lmliam.microsmith.gradle.worker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MicrosmithGradleWorkerFailureTests :
    StringSpec(
        {
            "describes a resolution failure with one coded line per issue" {
                val failure =
                    MicrosmithGradleWorkerFailure(
                        diagnostics =
                            listOf(
                                MicrosmithGradleWorkerDiagnostic(message = "first problem", code = "protobuf.first"),
                                MicrosmithGradleWorkerDiagnostic(message = "second problem", code = "protobuf.second"),
                            ),
                        type = "RESOLUTION",
                    )

                failure.describe() shouldBe
                    """
                    Microsmith model resolution failed:
                    [protobuf.first] first problem
                    [protobuf.second] second problem
                    """
                        .trimIndent()
            }

            "describes other failures with their type and plain messages" {
                val failure =
                    MicrosmithGradleWorkerFailure(
                        diagnostics = listOf(MicrosmithGradleWorkerDiagnostic("broken script")),
                        type = "COMPILATION",
                    )

                failure.describe() shouldBe "Microsmith generation failed (compilation).\nbroken script"
            }
        },
    )
