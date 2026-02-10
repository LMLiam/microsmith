package me.liam.microsmith.runtime.scripting

import kotlinx.coroutines.runBlocking
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
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import java.nio.file.Files
import java.nio.file.Path
import java.net.URLClassLoader

class MicrosmithScriptHost(
    private val cacheDirectory: Path = defaultCacheDirectory()
) {
    fun run(request: ScriptRunRequest): ScriptRunResult {
        val scriptPath = request.script.toAbsolutePath().normalize()
        val outputPath = request.outputDir.toAbsolutePath().normalize()
        val validationFailure = validateScriptPath(scriptPath)

        return validationFailure ?: runScript(request, scriptPath, outputPath)
    }

    private fun runScript(
        request: ScriptRunRequest,
        scriptPath: Path,
        outputPath: Path
    ): ScriptRunResult {
        val pluginClasspath = request.pluginClasspath.map { it.toAbsolutePath().normalize() }
        return withPluginClassLoader(pluginClasspath) { runtimeClassLoader ->
            runCatching {
                Files.createDirectories(cacheDirectory)
                val cache = MicrosmithScriptCache(cacheDirectory)
                val runtimeHostConfiguration = createRuntimeHostConfiguration(cache, runtimeClassLoader)
                val scriptContext = createScriptContext(outputPath, request)
                val (result, elapsedMillis) =
                    evaluateScript(
                        scriptPath = scriptPath,
                        runtimeHostConfiguration = runtimeHostConfiguration,
                        scriptContext = scriptContext,
                        pluginClasspath = pluginClasspath
                    )
                toRunResult(result, elapsedMillis, scriptContext, cache)
            }.getOrElse { exception ->
                val message = exception.message ?: exception::class.simpleName ?: "unknown error"
                ScriptRunFailure(
                    listOf(
                        "Unhandled script host failure: $message"
                    )
                )
            }
        }
    }

    private fun toRunResult(
        result: ResultWithDiagnostics<EvaluationResult>,
        elapsedMillis: Long,
        scriptContext: MicrosmithScriptContext,
        cache: MicrosmithScriptCache
    ): ScriptRunResult {
        val formattedReports = formatDiagnostics(result.reports)
        val hasErrors = formattedReports.any(::isErrorDiagnostic)

        return when (result) {
            is ResultWithDiagnostics.Failure ->
                ScriptRunFailure(
                    diagnostics = formattedReports.ifEmpty { listOf("Script compilation failed.") }
                )

            is ResultWithDiagnostics.Success ->
                if (hasErrors) {
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
    }

    private fun createRuntimeHostConfiguration(
        cache: MicrosmithScriptCache,
        runtimeClassLoader: ClassLoader
    ): ScriptingHostConfiguration =
        defaultJvmScriptingHostConfiguration.with {
            jvm {
                baseClassLoader(runtimeClassLoader)
                compilationCache(cache)
            }
        }

    private fun createScriptContext(
        outputPath: Path,
        request: ScriptRunRequest
    ): MicrosmithScriptContext =
        MicrosmithScriptContext(
            outDir = outputPath,
            vars = request.variables,
            flags = request.flags
        ) { model ->
            runBlocking {
                model.generateTo(outputPath)
            }
        }

    private fun evaluateScript(
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

private inline fun <T> withPluginClassLoader(
    pluginClasspath: List<Path>,
    block: (ClassLoader) -> T
): T {
    val parentClassLoader = MicrosmithScript::class.java.classLoader
    if (pluginClasspath.isEmpty()) {
        return withContextClassLoader(parentClassLoader) {
            block(parentClassLoader)
        }
    }

    val urls = pluginClasspath.map { it.toUri().toURL() }.toTypedArray()
    return URLClassLoader(urls, parentClassLoader).use { pluginClassLoader ->
        withContextClassLoader(pluginClassLoader) {
            block(pluginClassLoader)
        }
    }
}

private inline fun <T> withContextClassLoader(
    classLoader: ClassLoader,
    block: () -> T
): T {
    val thread = Thread.currentThread()
    val previous = thread.contextClassLoader
    thread.contextClassLoader = classLoader
    return try {
        block()
    } finally {
        thread.contextClassLoader = previous
    }
}

private fun validateScriptPath(scriptPath: Path) =
    when {
        !Files.exists(scriptPath) -> ScriptRunFailure(listOf("Script file '$scriptPath' does not exist."))
        !Files.isRegularFile(scriptPath) -> ScriptRunFailure(listOf("Script path '$scriptPath' is not a file."))
        else -> null
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
                is ResultValue.Error -> throw returnValue.error
                is ResultValue.Value -> {
                    val returnedModel = returnValue.value as? MicrosmithModel
                    when {
                        scriptContext.emittedAny() -> Unit
                        returnedModel != null -> scriptContext.emit(returnedModel)
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
            val message = error.message ?: error::class.simpleName ?: "unknown error"
            ScriptRunFailure(
                diagnostics =
                    warnings + listOf(
                        "Script evaluation failed: $message"
                    )
            )
        }
    )
}

private fun formatDiagnostics(reports: List<ScriptDiagnostic>): List<String> =
    reports
        .sortedWith(
            compareByDescending<ScriptDiagnostic> { it.severity }
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

private const val NANOS_PER_MILLISECOND = 1_000_000L
