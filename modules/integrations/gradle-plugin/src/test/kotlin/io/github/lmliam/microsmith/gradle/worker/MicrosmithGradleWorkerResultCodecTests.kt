package io.github.lmliam.microsmith.gradle.worker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class MicrosmithGradleWorkerResultCodecTests :
    StringSpec(
        {
            "round-trips issue codes of failure diagnostics" {
                val directory = Files.createTempDirectory("microsmith-gradle-worker-result-codec")
                try {
                    val codec = MicrosmithGradleWorkerResultCodec()
                    val failure =
                        MicrosmithGradleWorkerFailure(
                            diagnostics =
                                listOf(
                                    MicrosmithGradleWorkerDiagnostic(message = "coded", code = "protobuf.first"),
                                    MicrosmithGradleWorkerDiagnostic(message = "uncoded"),
                                ),
                            type = "RESOLUTION",
                        )
                    val resultFile = directory.resolve("result.properties")

                    codec.write(resultFile, failure)

                    codec.read(resultFile) shouldBe failure
                } finally {
                    runCatching { directory.deleteRecursively() }
                }
            }
        },
    )
