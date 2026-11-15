package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.gen.process.ProcessLauncher
import io.github.lmliam.microsmith.gen.process.ProcessResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class SystemProcessLauncher(private val ioDispatcher: CoroutineDispatcher) : ProcessLauncher {
    override suspend fun run(command: List<String>): ProcessResult =
        withContext(ioDispatcher) {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            coroutineScope {
                val output = async { process.inputStream.readBytes().decodeToString() }
                try {
                    ProcessResult(runInterruptible { process.waitFor() }, output.await())
                } finally {
                    process.destroyWithDescendants()
                    process.inputStream.close()
                }
            }
        }

    private fun Process.destroyWithDescendants() {
        val handles = listOf(toHandle()) + toHandle().descendants().toList()
        handles.forEach(ProcessHandle::destroy)
        val deadline = System.nanoTime() + DESTROY_GRACE_NANOS
        handles.forEach { handle ->
            try {
                handle.onExit().get(maxOf(0L, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)
            } catch (_: TimeoutException) {
                handle.destroyForcibly()
            }
        }
    }

    private companion object {
        val DESTROY_GRACE_NANOS = TimeUnit.SECONDS.toNanos(1)
    }
}
