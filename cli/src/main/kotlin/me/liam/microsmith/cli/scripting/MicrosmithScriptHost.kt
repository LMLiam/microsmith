package me.liam.microsmith.cli.scripting

import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.gen.helpers.generateTo
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import java.nio.file.Files
import java.nio.file.Path

internal fun executeMicrosmithScript(command: RunCommand): ScriptRunResult =
    MicrosmithScriptHost().run(
        ScriptRunRequest(
            script = command.script,
            outputDir = command.outputDir,
            variables = command.variables,
            flags = command.flags
        )
    )

internal class MicrosmithScriptHost(
    private val cacheDirectory: Path = defaultCacheDirectory()
) {
    fun run(request: ScriptRunRequest): ScriptRunResult {
        val scriptPath = request.script.toAbsolutePath().normalize()
        val outputPath = request.outputDir.toAbsolutePath().normalize()

        if (!Files.exists(scriptPath)) {
            return ScriptRunFailure(listOf("Script file '$scriptPath' does not exist."))
        }
        if (!Files.isRegularFile(scriptPath)) {
            return ScriptRunFailure(listOf("Script path '$scriptPath' is not a file."))
        }

        return try {
            Files.createDirectories(cacheDirectory)

            val cache = MicrosmithScriptCache(cacheDirectory)
            val runtimeHostConfiguration =
                defaultJvmScriptingHostConfiguration.with {
                    jvm {
                        baseClassLoader(MicrosmithScript::class.java.classLoader)
                        compilationCache(cache)
                    }
                }

            val host = BasicJvmScriptingHost(runtimeHostConfiguration)
            val scriptContext =
                MicrosmithScriptContext(
                    outDir = outputPath,
                    vars = request.variables,
                    flags = request.flags
                ) { model ->
                    kotlinx.coroutines.runBlocking {
                        model.generateTo(outputPath)
                    }
                }

            val compilationConfiguration =
                ScriptCompilationConfiguration(MicrosmithScriptCompilationConfiguration) {
                    hostConfiguration.update { runtimeHostConfiguration }
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
            val elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000

            val formattedReports = formatDiagnostics(result.reports)
            val errorDiagnostics = formattedReports.filter(::isErrorDiagnostic)

            when (result) {
                is ResultWithDiagnostics.Failure ->
                    ScriptRunFailure(
                        diagnostics =
                            if (formattedReports.isNotEmpty()) {
                                formattedReports
                            } else {
                                listOf("Script compilation failed.")
                            }
                    )

                is ResultWithDiagnostics.Success ->
                    if (errorDiagnostics.isNotEmpty()) {
                        ScriptRunFailure(formattedReports)
                    } else {
                        finalizeRun(
                            evaluationResult = result.value,
                            scriptContext = scriptContext,
                            warnings = formattedReports,
                            cacheHit = cache.retrievedScripts > 0,
                            elapsedMillis = elapsedMillis
                        )
                    }
            }
        } catch (error: Throwable) {
            ScriptRunFailure(
                listOf(
                    "Unhandled script host failure: ${error.message ?: error::class.simpleName ?: "unknown error"}"
                )
            )
        }
    }
}

private fun finalizeRun(
    evaluationResult: EvaluationResult,
    scriptContext: MicrosmithScriptContext,
    warnings: List<String>,
    cacheHit: Boolean,
    elapsedMillis: Long
): ScriptRunResult {
    val ensureModelGenerated =
        runCatching {
            when (val returnValue = evaluationResult.returnValue) {
                is ResultValue.Error -> {
                    throw returnValue.error
                }

                is ResultValue.Value -> {
                    val returnedModel = returnValue.value as? MicrosmithModel
                    when {
                        scriptContext.emittedAny() -> {
                            // Script called emit(...) explicitly; no additional action required.
                        }
                        returnedModel != null -> {
                            scriptContext.emit(returnedModel)
                        }
                        else -> {
                            error(
                                "Script must either return MicrosmithModel " +
                                    "or call emit(model)/generate(model)."
                            )
                        }
                    }
                }

                is ResultValue.Unit,
                ResultValue.NotEvaluated -> {
                    if (!scriptContext.emittedAny()) {
                        error(
                            "Script must either return MicrosmithModel " +
                                "or call emit(model)/generate(model)."
                        )
                    }
                }
            }
        }

    return ensureModelGenerated.fold(
        onSuccess = {
            ScriptRunSuccess(
                warnings = warnings,
                cacheHit = cacheHit,
                elapsedMillis = elapsedMillis
            )
        },
        onFailure = { error ->
            ScriptRunFailure(
                diagnostics =
                    warnings + listOf(
                        "Script evaluation failed: ${error.message ?: error::class.simpleName ?: "unknown error"}"
                    )
            )
        }
    )
}

private fun formatDiagnostics(reports: List<ScriptDiagnostic>): List<String> =
    reports
        .sortedWith(
            compareByDescending<ScriptDiagnostic> { severityRank(it.severity) }
                .thenBy { it.sourcePath.orEmpty() }
                .thenBy { it.location?.start?.line ?: Int.MAX_VALUE }
                .thenBy { it.location?.start?.col ?: Int.MAX_VALUE }
                .thenBy { it.message }
        ).map { report ->
            val severity = report.severity.name.lowercase()
            val source = report.sourcePath?.let(::shortPath) ?: "<script>"
            val line = report.location?.start?.line
            val column = report.location?.start?.col
            val locationSuffix =
                if (line != null && column != null) {
                    "$source:$line:$column"
                } else {
                    source
                }
            "[$severity] $locationSuffix ${report.message}"
        }

private fun severityRank(severity: ScriptDiagnostic.Severity): Int =
    when (severity) {
        ScriptDiagnostic.Severity.FATAL -> 5
        ScriptDiagnostic.Severity.ERROR -> 4
        ScriptDiagnostic.Severity.WARNING -> 3
        ScriptDiagnostic.Severity.INFO -> 2
        ScriptDiagnostic.Severity.DEBUG -> 1
    }

private fun isErrorDiagnostic(line: String): Boolean = line.startsWith("[error]") || line.startsWith("[fatal]")

private fun shortPath(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')

private fun defaultCacheDirectory(): Path {
    val envPath =
        System.getenv("MICROSMITH_SCRIPT_CACHE_DIR")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    if (envPath != null) {
        return Path.of(envPath)
    }

    return Path.of(System.getProperty("user.home"), ".microsmith", "cache", "scripts")
}
