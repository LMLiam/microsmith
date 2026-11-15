package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScript
import io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScriptCompilationConfiguration
import io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScriptEvaluationConfiguration
import java.nio.file.Path
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
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.time.TimeSource

@Inject
internal class ScriptEvaluator(private val timeSource: TimeSource) {
    fun evaluate(
        scriptPath: Path,
        runtimeHostConfiguration: ScriptingHostConfiguration,
        scriptContext: MicrosmithScriptContext,
        pluginClasspath: List<Path>,
    ): Pair<ResultWithDiagnostics<EvaluationResult>, Long> {
        val host = BasicJvmScriptingHost(runtimeHostConfiguration)
        val hostClasspath =
            scriptCompilationClasspathFromContext(
                classLoader = MicrosmithScript::class.java.classLoader,
                wholeClasspath = true,
            )
        val compilationConfiguration =
            ScriptCompilationConfiguration(MicrosmithScriptCompilationConfiguration) {
                hostConfiguration.update { runtimeHostConfiguration }
                jvm { updateClasspath(hostClasspath + pluginClasspath.map(Path::toFile)) }
            }
        val evaluationConfiguration =
            ScriptEvaluationConfiguration(MicrosmithScriptEvaluationConfiguration) {
                hostConfiguration.update { runtimeHostConfiguration }

                implicitReceivers(scriptContext)
            }
        val startMark = timeSource.markNow()
        val result =
            host.eval(
                script = FileScriptSource(scriptPath.toFile()),
                compilationConfiguration = compilationConfiguration,
                evaluationConfiguration = evaluationConfiguration,
            )
        return result to startMark.elapsedNow().inWholeMilliseconds
    }
}
