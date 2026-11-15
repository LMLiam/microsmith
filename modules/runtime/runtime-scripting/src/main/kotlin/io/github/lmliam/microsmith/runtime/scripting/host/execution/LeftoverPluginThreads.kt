package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal const val PLUGIN_THREAD_LEFT_RUNNING_CODE = "microsmith.script.plugin-thread-left-running"

private val INTERRUPT_GRACE_PERIOD = 2.seconds

@Inject
internal class LeftoverPluginThreads(
    private val request: ScriptRunRequest,
    private val classLoader: ScriptRunClassLoader,
    private val diagnostics: DiagnosticSink,
    private val timeSource: TimeSource,
) {
    fun stop() {
        val pluginLoader = classLoader.pluginLoader ?: return
        val leftovers = Thread.getAllStackTraces().keys.filter { it.descendsFrom(pluginLoader) }
        if (leftovers.isEmpty()) return

        leftovers.forEach(Thread::interrupt)
        val deadline = timeSource.markNow() + INTERRUPT_GRACE_PERIOD
        leftovers.forEach { it.join(maxOf(1L, -deadline.elapsedNow().inWholeMilliseconds)) }
        leftovers.filter(Thread::isAlive).forEach { thread ->
            diagnostics.report(
                Diagnostic(
                    severity = DiagnosticSeverity.WARNING,
                    code = PLUGIN_THREAD_LEFT_RUNNING_CODE,
                    message =
                        "Plugin thread '${thread.name}' did not stop when interrupted and keeps the plugin class " +
                            "loader reachable. Plugin class path: " +
                            request.pluginClasspath.joinToString(File.pathSeparator),
                ),
            )
        }
    }
}

private fun Thread.descendsFrom(loader: ClassLoader): Boolean =
    generateSequence(contextClassLoader) { it.parent }.any { it === loader }
