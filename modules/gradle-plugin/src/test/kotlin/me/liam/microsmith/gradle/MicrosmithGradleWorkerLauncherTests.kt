package me.liam.microsmith.gradle

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class MicrosmithGradleWorkerLauncherTests :
    StringSpec({
        "launcher reads the worker result written for the current execution" {
            val workDirectory = Files.createTempDirectory("microsmith-gradle-worker-launcher-success")
            try {
                val resultCodec = MicrosmithGradleWorkerResultCodec()
                val launcher =
                    MicrosmithGradleWorkerLauncher(
                        resultCodec = resultCodec,
                        processExecutor = MicrosmithGradleWorkerProcessExecutor { command ->
                            val resultFile = Path.of(command.last())
                            resultCodec.write(
                                resultFile,
                                MicrosmithGradleWorkerSuccess(
                                    warnings = listOf("warning"),
                                    cacheHit = true,
                                    elapsedMillis = 12,
                                ),
                            )
                            MicrosmithGradleWorkerProcessOutcome(exitCode = 0, processOutput = "")
                        },
                    )

                val result = launcher.execute(sampleRequest(), workDirectory, sampleRuntimeClasspath())

                result shouldBe MicrosmithGradleWorkerSuccess(
                    warnings = listOf("warning"),
                    cacheHit = true,
                    elapsedMillis = 12,
                )
            } finally {
                workDirectory.deleteRecursively()
            }
        }

        "launcher ignores stale result files from previous executions" {
            val workDirectory = Files.createTempDirectory("microsmith-gradle-worker-launcher-stale")
            try {
                val staleWorkspace = Files.createDirectories(workDirectory.resolve("microsmith-worker"))
                MicrosmithGradleWorkerResultCodec().write(
                    staleWorkspace.resolve("result.properties"),
                    MicrosmithGradleWorkerSuccess(
                        warnings = emptyList(),
                        cacheHit = true,
                        elapsedMillis = 1,
                    ),
                )
                val launcher =
                    MicrosmithGradleWorkerLauncher(
                        processExecutor = MicrosmithGradleWorkerProcessExecutor {
                            MicrosmithGradleWorkerProcessOutcome(
                                exitCode = 2,
                                processOutput = "worker crashed before writing a result",
                            )
                        },
                    )

                val failure =
                    shouldThrow<GradleException> {
                        launcher.execute(sampleRequest(), workDirectory, sampleRuntimeClasspath())
                    }

                failure.message.shouldContain("Worker exit code: 2")
                failure.message.shouldContain("worker crashed before writing a result")
            } finally {
                workDirectory.deleteRecursively()
            }
        }
    })

private fun sampleRequest(): MicrosmithGradleWorkerRequest = MicrosmithGradleWorkerRequest(
    scriptPath = Path.of("/tmp/build.microsmith.kts"),
    outputPath = Path.of("/tmp/generated"),
    cacheDirectory = Path.of("/tmp/cache"),
    variables = mapOf("entityName" to "WorkerUserCreated"),
    flags = setOf("emit"),
    pluginClasspath = emptyList(),
)

private fun sampleRuntimeClasspath(): List<Path> = listOf(Path.of("/tmp/runtime-scripting.jar"))
