package me.liam.microsmith.cli

import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlinx.cli.*
import me.liam.microsmith.scripting.MavenScriptDependencyResolver
import me.liam.microsmith.scripting.MicrosmithScriptException
import me.liam.microsmith.scripting.MicrosmithScriptHost
import me.liam.microsmith.scripting.ScriptDependencyResolver
import me.liam.microsmith.scripting.ScriptOptions

fun main(args: Array<String>) {
    val code = MicrosmithCli().run(args.toList())
    exitProcess(code)
}

data class CliStatus(
    val code: Int,
    override val message: String? = null
) : RuntimeException(message)

class MicrosmithCli(
    private val scriptHost: MicrosmithScriptHost = MicrosmithScriptHost,
    private val dependencyResolver: ScriptDependencyResolver = MavenScriptDependencyResolver()
) {
    fun run(args: List<String>): Int {
        val parser = ArgParser("microsmith")
        parser.subcommands(
            RunCommand(scriptHost, dependencyResolver)
        )

        return try {
            parser.parse(args.toTypedArray())
            0
        } catch (e: CliStatus) {
            e.message?.let { System.err.println(it) }
            e.code
        } catch (e: Exception) {
            System.err.println(e.message ?: "Unexpected error")
            4
        }
    }
}

class RunCommand(
    private val scriptHost: MicrosmithScriptHost,
    private val dependencyResolver: ScriptDependencyResolver
) : Subcommand("run", "Run a microsmith.kts script and generate artifacts") {
    private val script by argument(
        ArgType.String,
        description = "Path to microsmith.kts or .microsmith.kts file"
    )
    private val outputDir by option(
        ArgType.String,
        fullName = "out",
        shortName = "o",
        description = "Output directory for generated files"
    ).default("build/microsmith-out")
    private val plugins by option(
        ArgType.String,
        fullName = "plugin",
        description = "Additional script or generator plugin (Maven coordinate)"
    ).multiple()
    private val generatorFilters by option(
        ArgType.String,
        fullName = "generator",
        description = "Limit generators by id (e.g. schemas, protobuf)"
    ).multiple()
    private val jsonSummary by option(
        ArgType.Boolean,
        fullName = "json-summary",
        description = "Print a JSON summary of generators and outputs"
    ).default(false)

    override fun execute() {
        val normalizedScript = Paths.get(script).toAbsolutePath().normalize()
        val normalizedOut = Paths.get(outputDir).toAbsolutePath().normalize()
        val requested = generatorFilters.map { it.lowercase() }.toSet().takeIf { it.isNotEmpty() }

        val pluginClasspath =
            try {
                dependencyResolver.resolve(plugins)
            } catch (ex: Exception) {
                throw CliStatus(4, "Unable to resolve plugins: ${ex.message}")
            }

        val baseClasspath =
            (MicrosmithScriptHost.currentClasspath() + pluginClasspath)
                .distinct()
                .map { it.toAbsolutePath().normalize() }

        val classLoader =
            URLClassLoader(
                baseClasspath.map { it.toUri().toURL() }.toTypedArray(),
                this::class.java.classLoader
            )

        try {
            val scriptOptions =
                ScriptOptions(
                    extraClasspath = pluginClasspath,
                    dependencyResolver = ScriptDependencyResolver.None,
                    pluginDependencies = emptyList(),
                    allowPlainKts = true,
                    baseClassLoader = classLoader
                )

            val model =
                try {
                    scriptHost.evaluate(normalizedScript, baseClasspath, scriptOptions)
                } catch (ex: MicrosmithScriptException) {
                    System.err.println(ex.message ?: "Script failed")
                    throw CliStatus(2)
                } catch (ex: Exception) {
                    System.err.println("Script failed: ${ex.message}")
                    throw CliStatus(2)
                }

            val executor = GeneratorExecutor(classLoader) { println(it) }
            val runs =
                try {
                    executor.runGenerators(model, normalizedOut, requested)
                } catch (ex: IllegalArgumentException) {
                    throw CliStatus(4, ex.message ?: "Invalid generator filter")
                } catch (ex: GenerationFailure) {
                    System.err.println(
                        "Generation failed for ${ex.generatorId}: ${ex.cause?.message ?: ex.message}"
                    )
                    throw CliStatus(3)
                }

            if (jsonSummary) {
                println(renderJsonSummary(runs, normalizedOut))
            } else {
                runs.forEach { run -> println("Generator '${run.id}' wrote ${run.files.size} file(s)") }
                println("Outputs written to $normalizedOut")
            }
        } finally {
            (classLoader as? URLClassLoader)?.close()
        }
    }

    private fun renderJsonSummary(
        runs: List<GeneratorRun>,
        base: Path
    ): String {
        val generators =
            runs.joinToString(",") { run ->
                val files =
                    run.files.joinToString(",") { file ->
                        val relative =
                            try {
                                base.relativize(file).toString().replace("\\", "/")
                            } catch (_: Exception) {
                                file.toString().replace("\\", "/")
                            }
                        "\"${escapeJson(relative)}\""
                    }
                """{"id":"${escapeJson(run.id)}","files":[$files]}"""
            }
        return """{"generators":[${generators}]}"""
    }

    private fun escapeJson(value: String) =
        buildString {
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    else -> append(ch)
                }
            }
        }
}
