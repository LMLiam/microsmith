package io.github.lmliam.microsmith.gen.system

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
                val directory = Files.createTempDirectory("microsmith-process-")
                val pidFile = directory.resolve("pid")
                val program = directory.resolve("Sleep.java")
                Files.writeString(
                    program,
                    "public class Sleep { public static void main(String[] args) throws Exception {" +
                        " java.nio.file.Files.writeString(java.nio.file.Path.of(args[0])," +
                        " String.valueOf(ProcessHandle.current().pid()));" +
                        " Thread.sleep(600_000); } }",
                )

                runBlocking {
                    val run =
                        CoroutineScope(dispatcher).launch {
                            launcher.run(listOf(java, program.toString(), pidFile.toString()))
                        }
                    val pid = withTimeout(30.seconds) { awaitPid(pidFile) }

                    withTimeout(10.seconds) { run.cancelAndJoin() }

                    ProcessHandle.of(pid).ifPresent { it.onExit().get(10, TimeUnit.SECONDS) }
                    ProcessHandle.of(pid).map { it.isAlive }.orElse(false) shouldBe false
                }
            }

            "cancelling run does not wait for a detached process that keeps the output open" {
                val directory = Files.createTempDirectory("microsmith-process-")
                val program = directory.resolve("Detach.java")
                Files.writeString(
                    program,
                    "public class Detach { public static void main(String[] args) throws Exception {" +
                        " if (args[0].equals(\"child\")) { Thread.sleep(20_000); return; }" +
                        " new ProcessBuilder(ProcessHandle.current().info().command().get()," +
                        " args[0], \"child\").inheritIO().start(); } }",
                )

                runBlocking {
                    val run =
                        CoroutineScope(dispatcher).launch {
                            launcher.run(listOf(java, program.toString(), program.toString()))
                        }
                    delay(2.seconds)

                    withTimeout(10.seconds) { run.cancelAndJoin() }
                }
            }
        },
    )

private suspend fun awaitPid(pidFile: Path): Long {
    while (!Files.exists(pidFile) || Files.size(pidFile) == 0L) delay(50)
    return Files.readString(pidFile).toLong()
}
