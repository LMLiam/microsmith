package me.liam.microsmith.cli

import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.schemas.SchemaEmitter
import java.nio.file.Path
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import kotlin.system.exitProcess

private const val RUN_COMMAND = "run"
private const val OUTPUT_OPTION = "--out"
private const val SCRIPT_EXTENSION = ".microsmith.kts"
private val HELP_COMMANDS = setOf("--help", "-h", "help")

private const val HELP_TEXT = """
Microsmith CLI (Phase 1)

Usage:
  microsmith run <script.microsmith.kts> --out <output-dir>
  microsmith --help
"""

fun main(args: Array<String>) {
    exitProcess(MicrosmithCli().run(args))
}

class MicrosmithCli(
    private val stdout: (String) -> Unit = ::println,
    private val stderr: (String) -> Unit = { System.err.println(it) },
    private val providerValidator: () -> List<String> = ::verifyBuiltinProviders
) {
    fun run(args: Array<String>): Int =
        when (val parsed = parseCliArgs(args.toList())) {
            is CliCommand.Help -> {
                stdout(HELP_TEXT.trimIndent())
                0
            }
            is CliCommand.Error -> {
                stderr(parsed.message)
                stderr("")
                stderr(HELP_TEXT.trimIndent())
                2
            }
            is CliCommand.Run -> runCommand(parsed)
        }

    private fun runCommand(command: CliCommand.Run): Int {
        val providerErrors =
            when (val validation = validateProviders()) {
                is ProviderValidation.Failed -> listOf(validation.message)
                is ProviderValidation.Loaded -> validation.errors
            }

        if (providerErrors.isEmpty()) {
            stdout(
                "Phase 1 scaffold complete. " +
                    "Script execution will be added in Phase 2. " +
                    "script='${command.script}', out='${command.outputDir}'."
            )
        } else {
            providerErrors.forEach(stderr)
        }

        return if (providerErrors.isEmpty()) 0 else 2
    }

    private fun validateProviders(): ProviderValidation =
        try {
            ProviderValidation.Loaded(providerValidator())
        } catch (error: ServiceConfigurationError) {
            val message = error.message ?: error::class.simpleName ?: "ServiceConfigurationError"
            ProviderValidation.Failed("Failed to load runtime service providers: $message")
        }
}

private sealed interface ProviderValidation {
    data class Loaded(
        val errors: List<String>
    ) : ProviderValidation

    data class Failed(
        val message: String
    ) : ProviderValidation
}

private sealed interface CliParseResult<out T> {
    data class Success<T>(
        val value: T
    ) : CliParseResult<T>

    data class Failure(
        val message: String
    ) : CliParseResult<Nothing>
}

private data class RunArguments(
    val script: Path,
    val outputDir: Path
)

private fun CliParseResult.Failure.toCliError() = CliCommand.Error(message)

private fun parseRunArgs(args: List<String>): CliParseResult<RunArguments> {
    val scriptResult = parseScriptArg(args.getOrNull(1))
    val script =
        when (scriptResult) {
            is CliParseResult.Failure -> return scriptResult
            is CliParseResult.Success -> scriptResult.value
        }

    return when (val outputDirResult = parseOutputDir(args, startIndex = 2)) {
        is CliParseResult.Success -> {
            CliParseResult.Success(RunArguments(script = script, outputDir = outputDirResult.value))
        }
        is CliParseResult.Failure -> outputDirResult
    }
}

private fun parseScriptArg(scriptArg: String?): CliParseResult<Path> {
    val result =
        when {
            scriptArg == null || scriptArg.startsWith("--") ->
                CliParseResult.Failure("Missing <script.microsmith.kts> argument for run command.")

            !scriptArg.endsWith(SCRIPT_EXTENSION) ->
                CliParseResult.Failure("Script file must use the .microsmith.kts extension.")

            else -> CliParseResult.Success(Path.of(scriptArg))
        }
    return result
}

private fun parseOutputDir(
    args: List<String>,
    startIndex: Int
): CliParseResult<Path> {
    var outputDir: Path? = null
    var index = startIndex
    var error: String? = null

    while (index < args.size && error == null) {
        val token = args[index]
        if (token == OUTPUT_OPTION) {
            val value = args.getOrNull(index + 1)
            error =
                when {
                    value == null || value.startsWith("--") -> "Missing value for --out option."
                    outputDir != null -> "--out option may only be specified once."
                    else -> null
                }

            if (error == null && value != null) {
                outputDir = Path.of(value)
                index += 2
            }
        } else {
            error = "Unknown option '$token'."
        }

        if (token != OUTPUT_OPTION || error != null) {
            index += 1
        }
    }

    val result =
        when {
            error != null -> CliParseResult.Failure(error)
            outputDir == null -> CliParseResult.Failure("Missing required --out <output-dir> option.")
            else -> CliParseResult.Success(outputDir)
        }
    return result
}

internal sealed interface CliCommand {
    data object Help : CliCommand

    data class Run(
        val script: Path,
        val outputDir: Path
    ) : CliCommand

    data class Error(
        val message: String
    ) : CliCommand
}

internal fun parseCliArgs(args: List<String>): CliCommand {
    val command = args.firstOrNull()
    return when {
        command == null || command in HELP_COMMANDS -> CliCommand.Help
        command != RUN_COMMAND -> CliCommand.Error("Unknown command '$command'.")
        else ->
            when (val parsed = parseRunArgs(args)) {
                is CliParseResult.Success -> CliCommand.Run(script = parsed.value.script, outputDir = parsed.value.outputDir)
                is CliParseResult.Failure -> parsed.toCliError()
            }
        }
}

internal fun verifyBuiltinProviders(
    modelGenerators: List<ModelGenerator<*>> = loadModelGenerators(),
    schemaEmitters: List<SchemaEmitter<*>> = loadSchemaEmitters()
): List<String> {
    val errors = mutableListOf<String>()

    if (modelGenerators.none { it.extension == SchemasExtension::class }) {
        errors += "Missing built-in ModelGenerator for SchemasExtension. Check CLI runtime packaging."
    }

    if (schemaEmitters.none { it.type == ProtobufSchema::class }) {
        errors += "Missing built-in SchemaEmitter for ProtobufSchema. Check CLI runtime packaging."
    }

    return errors
}

private fun loadModelGenerators() = ServiceLoader.load(ModelGenerator::class.java).iterator().asSequence().toList()

private fun loadSchemaEmitters() = ServiceLoader.load(SchemaEmitter::class.java).iterator().asSequence().toList()
