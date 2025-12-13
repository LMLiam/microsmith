package me.liam.microsmith.scripting

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.dependencies.ArtifactWithLocation
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import kotlinx.coroutines.runBlocking
import me.liam.microsmith.dsl.core.MicrosmithBuilder
import me.liam.microsmith.dsl.core.MicrosmithModel

data class ScriptOptions(
    val pluginDependencies: List<String> = emptyList(),
    val dependencyResolver: ScriptDependencyResolver = ScriptDependencyResolver.None,
    val extraClasspath: List<Path> = emptyList(),
    val cacheDir: Path? = null,
    val cacheScripts: Boolean = true,
    val allowPlainKts: Boolean = true,
    val strictMarker: String = "// microsmith",
    val baseClassLoader: ClassLoader? = null
)

fun interface ScriptDependencyResolver {
    fun resolve(coordinates: List<String>): List<Path>

    companion object {
        val None = ScriptDependencyResolver { emptyList() }
    }
}

class MavenScriptDependencyResolver(
    private val delegate: ExternalDependenciesResolver = MavenDependenciesResolver()
) : ScriptDependencyResolver {
    override fun resolve(coordinates: List<String>): List<Path> {
        if (coordinates.isEmpty()) return emptyList()

        val artifacts =
            coordinates.map { coordinate ->
                ArtifactWithLocation(
                    coordinate,
                    SourceCode.LocationWithId(
                        coordinate,
                        SourceCode.Location(
                            SourceCode.Position(0, 0),
                            null
                        )
                    )
                )
            }
        val result =
            runBlocking {
                delegate.resolve(artifacts, ExternalDependenciesResolver.Options.Empty)
            }

        return when (result) {
            is ResultWithDiagnostics.Success -> result.value.map { it.toPath() }
            is ResultWithDiagnostics.Failure ->
                throw MicrosmithScriptException(
                    buildString {
                        appendLine("Failed to resolve script plugins:")
                        result.reports.forEach { appendLine("- ${it.message}") }
                    }
                )
        }
    }
}

class MicrosmithScriptException(message: String) : RuntimeException(message)

object MicrosmithScriptHost {
    fun evaluate(
        scriptFile: Path,
        classpath: List<Path>,
        options: ScriptOptions = ScriptOptions()
    ): MicrosmithModel {
        val normalized = scriptFile.toAbsolutePath().normalize()
        if (!normalized.exists() || !normalized.isRegularFile()) {
            throw MicrosmithScriptException("Script file not found: $normalized")
        }

        val scriptText = normalized.readText()
        validateExtension(normalized, scriptText, options)

        val pluginPaths = options.dependencyResolver.resolve(options.pluginDependencies)
        val effectiveClasspath =
            (classpath + options.extraClasspath + pluginPaths)
                .distinct()
                .map { it.toAbsolutePath().normalize() }

        val cacheDir = options.cacheDir ?: defaultCacheDir()
        cacheDir.takeIf { options.cacheScripts }?.let { Files.createDirectories(it) }

        val compilationConfiguration =
            ScriptCompilationConfiguration(MicrosmithScriptDefinition) {
                jvm {
                    updateClasspath(effectiveClasspath.map(Path::toFile))
                }
            }

        val builder = MicrosmithBuilder()
        val evaluationConfiguration =
            ScriptEvaluationConfiguration {
                implicitReceivers(builder)
                options.baseClassLoader?.let { loader ->
                    jvm { baseClassLoader(loader) }
                }
            }

        val hostConfiguration =
            ScriptingHostConfiguration {
                jvm {
                    options.baseClassLoader?.let { loader -> baseClassLoader(loader) }
                    if (options.cacheScripts) {
                        val compiledDir = cacheDir.resolve("compiled")
                        Files.createDirectories(compiledDir)
                        compilationCache(
                            CompiledScriptJarsCache { source: SourceCode, _ ->
                                val key =
                                    source.locationId
                                        ?: source.name
                                        ?: normalized.toString()
                                compiledDir.resolve(key.hashCode().toString() + ".jar").toFile()
                            }
                        )
                    }
                }
            }

        val host = BasicJvmScriptingHost(hostConfiguration)

        val result =
            host.eval(
                normalized.toFile().toScriptSource(),
                compilationConfiguration,
                evaluationConfiguration
            )

        return when (result) {
            is ResultWithDiagnostics.Failure -> throw MicrosmithScriptException(renderDiagnostics(result, scriptText))
            is ResultWithDiagnostics.Success -> extractModel(result)
        }
    }

    fun currentClasspath(): List<Path> =
        System.getProperty("java.class.path")
            ?.split(File.pathSeparator)
            ?.filter { it.isNotBlank() }
            ?.map { Paths.get(it) }
            ?.toList()
            ?: emptyList()

    fun defaultCacheDir(): Path =
        runCatching { Paths.get(System.getProperty("user.home"), ".cache", "microsmith") }
            .getOrElse { Paths.get("build", "microsmith", "cache") }

    private fun validateExtension(
        scriptFile: Path,
        scriptText: String,
        options: ScriptOptions
    ) {
        val name = scriptFile.fileName.toString()
        if (name.endsWith(".microsmith.kts")) return
        if (name.endsWith(".kts") && options.allowPlainKts) {
            val markerPresent = scriptText.lineSequence().any { it.contains(options.strictMarker) }
            if (!markerPresent) {
                throw MicrosmithScriptException(
                    "Plain .kts scripts must contain the marker '${options.strictMarker}' to opt into Microsmith evaluation."
                )
            }
            return
        }

        throw MicrosmithScriptException("Unsupported script extension for $name. Use .microsmith.kts")
    }

    private fun renderDiagnostics(
        failure: ResultWithDiagnostics.Failure,
        scriptText: String
    ): String {
        val lines = scriptText.lines()
        return buildString {
            appendLine("Script compilation failed:")
            failure.reports
                .filter { it.severity >= ScriptDiagnostic.Severity.ERROR }
                .forEach { diag ->
                    appendLine(renderDiagnostic(diag, lines))
                }
        }.trimEnd()
    }

    private fun renderDiagnostic(
        diagnostic: ScriptDiagnostic,
        lines: List<String>
    ): String {
        val start = diagnostic.location?.start
        val lineIndex = (start?.line?.takeIf { it > 0 } ?: 1) - 1
        val column = (start?.col?.takeIf { it > 0 } ?: 1)
        val line = lines.getOrNull(lineIndex).orEmpty()
        val caret =
            buildString {
                repeat((column - 1).coerceAtLeast(0)) { append(' ') }
                append('^')
            }

        val message = diagnostic.message
        return "line ${lineIndex + 1}:$column: $message\n$line\n$caret"
    }

    private fun extractModel(
        result: ResultWithDiagnostics.Success<EvaluationResult>
    ): MicrosmithModel {
        val returnValue = result.value.returnValue
        val direct =
            when (returnValue) {
                is ResultValue.Value -> returnValue.value
                is ResultValue.Unit -> null
                is ResultValue.Error -> throw MicrosmithScriptException("Script evaluation failed: ${returnValue.error}")
                else -> null
            }

        when (direct) {
            is MicrosmithModel -> return direct
            is MicrosmithBuilder -> return direct.model
        }

        val scriptInstance =
            when (returnValue) {
                is ResultValue.Unit -> returnValue.scriptInstance
                is ResultValue.Value -> returnValue.scriptInstance
                else -> null
            }

        val reflected =
            scriptInstance?.let { instance ->
                instance::class.memberProperties
                    .firstOrNull { it.name == "model" }
                    ?.apply { isAccessible = true }
                    ?.getter
                    ?.call(instance)
            }

        if (reflected is MicrosmithModel) return reflected
        if (reflected is MicrosmithBuilder) return reflected.model

        throw MicrosmithScriptException("Script completed without producing a MicrosmithModel. Return the model or expose a top-level 'model' property.")
    }
}
