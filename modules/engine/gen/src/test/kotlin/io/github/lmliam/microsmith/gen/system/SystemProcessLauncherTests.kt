package io.github.lmliam.microsmith.gen.system

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class SystemProcessLauncherTests :
    StringSpec(
        {
            val java = ProcessHandle.current().info().command().orElseThrow().toString()
            val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
            val launcher = SystemProcessLauncher(dispatcher)

            afterSpec { dispatcher.close() }

            "run returns the exit code and the output with standard error merged" {
                runBlocking {
                    val result = launcher.run(listOf(java, "-version"))

                    result.exitCode shouldBe 0
                    result.output shouldContain "version"
                }
            }

            "cancelling run destroys the process" {
                val pidFile = createTempDirectory("microsmith-process-").resolve("pid")

                runBlocking {
                    val run =
                        CoroutineScope(dispatcher).launch {
                            launcher.run(listOf(java, programPath("Sleep"), pidFile.toString()))
                        }
                    val pid = withTimeout(30.seconds) { awaitText(pidFile) }.toLong()

                    withTimeout(10.seconds) { run.cancelAndJoin() }

                    ProcessHandle.of(pid).ifPresent { it.onExit().get(10, TimeUnit.SECONDS) }
                    ProcessHandle.of(pid).map { it.isAlive }.orElse(false) shouldBe false
                }
            }

            "cancelling run does not wait for a detached process that keeps the output open" {
                val markerFile = createTempDirectory("microsmith-process-").resolve("started")

                runBlocking {
                    val program = programPath("Detach")
                    val run =
                        CoroutineScope(dispatcher).launch {
                            launcher.run(listOf(java, program, program, markerFile.toString()))
                        }
                    withTimeout(30.seconds) { awaitText(markerFile) }

                    withTimeout(10.seconds) { run.cancelAndJoin() }
                }
            }
        },
    )

private fun programPath(name: String): String =
    Path.of(checkNotNull(SystemProcessLauncherTests::class.java.getResource("/process/$name.java")).toURI()).toString()

private suspend fun awaitText(file: Path): String {
    while (!file.exists() || file.fileSize() == 0L) delay(50)
    return file.readText()
}
