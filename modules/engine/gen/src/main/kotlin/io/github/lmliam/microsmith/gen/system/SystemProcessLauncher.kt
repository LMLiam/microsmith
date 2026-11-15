package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.gen.process.ProcessLauncher
import io.github.lmliam.microsmith.gen.process.ProcessResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

class SystemProcessLauncher(private val ioDispatcher: CoroutineDispatcher) : ProcessLauncher {
    override suspend fun run(command: List<String>): ProcessResult =
        withContext(ioDispatcher) {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output = CoroutineScope(ioDispatcher).async { process.inputStream.readBytes().decodeToString() }
            try {
                ProcessResult(runInterruptible { process.waitFor() }, output.await())
            } finally {
                process.destroyWithDescendants()
                process.inputStream.close()
            }
        }

    private fun Process.destroyWithDescendants() {
        val handles = listOf(toHandle()) + toHandle().descendants().toList()
        handles.forEach(ProcessHandle::destroy)
        try {
            handles
                .map { it.onExit() }
                .toTypedArray()
                .let(CompletableFuture<*>::allOf)
                .get(DESTROY_GRACE_SECONDS, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            handles.forEach(ProcessHandle::destroyForcibly)
        }
    }

    private companion object {
        const val DESTROY_GRACE_SECONDS = 1L
    }
}
