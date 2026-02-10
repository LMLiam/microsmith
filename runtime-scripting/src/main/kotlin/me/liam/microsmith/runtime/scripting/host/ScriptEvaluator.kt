package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import me.liam.microsmith.runtime.scripting.definition.MicrosmithScriptCompilationConfiguration
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import java.nio.file.Path

internal object ScriptEvaluator {
    fun evaluate(
        scriptPath: Path,
        runtimeHostConfiguration: ScriptingHostConfiguration,
        scriptContext: MicrosmithScriptContext,
        pluginClasspath: List<Path>
    ): Pair<ResultWithDiagnostics<EvaluationResult>, Long> {
        val host = BasicJvmScriptingHost(runtimeHostConfiguration)
        val compilationConfiguration =
            ScriptCompilationConfiguration(MicrosmithScriptCompilationConfiguration) {
                hostConfiguration.update { runtimeHostConfiguration }
                jvm {
                    updateClasspath(pluginClasspath.map(Path::toFile))
                }
            }
        val evaluationConfiguration =
            ScriptEvaluationConfiguration {
                hostConfiguration.update { runtimeHostConfiguration }
                implicitReceivers(scriptContext)
            }
        val startNanos = System.nanoTime()
        val result =
            host.eval(
                script = FileScriptSource(scriptPath.toFile()),
                compilationConfiguration = compilationConfiguration,
                evaluationConfiguration = evaluationConfiguration
            )
        val elapsedMillis = (System.nanoTime() - startNanos) / NANOS_PER_MILLISECOND
        return result to elapsedMillis
    }
}

private const val NANOS_PER_MILLISECOND = 1_000_000L
