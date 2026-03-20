package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import java.nio.file.Path

internal class RunOptionsState {
    var outputDir: Path? = null
    val variables = linkedMapOf<String, String>()
    val flags = linkedSetOf<String>()
    val plugins = linkedSetOf<String>()
    val pluginJars = linkedSetOf<Path>()
    var offline: Boolean = false
    var repositoryOverride: String? = null
    var isolationModeSpecified: Boolean = false
    var isolationMode: ScriptIsolationMode = ScriptIsolationMode.CLASSLOADER
    var diagnosticsFormatSpecified: Boolean = false
    var diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT
    var verbose: Boolean = false
    var eventLog: Path? = null
    var error: String? = null

    fun toParsedRunOptions(): ParsedRunOptions = ParsedRunOptions(
        outputDir = outputDir ?: Path.of("."),
        variables = variables.toMap(),
        flags = flags.toSet(),
        plugins = plugins.toSet(),
        pluginJars = pluginJars.toSet(),
        offline = offline,
        repositoryOverride = repositoryOverride,
        isolationMode = isolationMode,
        diagnosticsFormat = diagnosticsFormat,
        verbose = verbose,
        eventLog = eventLog,
        error = error,
    )
}
