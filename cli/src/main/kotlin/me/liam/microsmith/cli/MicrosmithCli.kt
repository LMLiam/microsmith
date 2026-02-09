package me.liam.microsmith.cli

import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.schemas.SchemaEmitter
import java.nio.file.Path
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import kotlin.system.exitProcess

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
            try {
                providerValidator()
            } catch (error: ServiceConfigurationError) {
                val message = error.message ?: error::class.simpleName ?: "ServiceConfigurationError"
                stderr("Failed to load runtime service providers: $message")
                return 2
            }
        if (providerErrors.isNotEmpty()) {
            providerErrors.forEach(stderr)
            return 2
        }

        stdout(
            "Phase 1 scaffold complete. " +
                "Script execution will be added in Phase 2. " +
                "script='${command.script}', out='${command.outputDir}'."
        )
        return 0
    }
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
    if (args.isEmpty() || args[0] in setOf("--help", "-h", "help")) {
        return CliCommand.Help
    }

    if (args[0] != "run") {
        return CliCommand.Error("Unknown command '${args[0]}'.")
    }

    if (args.size < 2 || args[1].startsWith("--")) {
        return CliCommand.Error("Missing <script.microsmith.kts> argument for run command.")
    }

    val script = Path.of(args[1])
    if (!script.fileName.toString().endsWith(".microsmith.kts")) {
        return CliCommand.Error("Script file must use the .microsmith.kts extension.")
    }
    var outputDir: Path? = null
    var index = 2
    while (index < args.size) {
        val token = args[index]
        when (token) {
            "--out" -> {
                val value = args.getOrNull(index + 1)
                if (value == null || value.startsWith("--")) {
                    return CliCommand.Error("Missing value for --out option.")
                }
                if (outputDir != null) {
                    return CliCommand.Error("--out option may only be specified once.")
                }
                outputDir = Path.of(value)
                index += 2
            }
            else -> return CliCommand.Error("Unknown option '$token'.")
        }
    }

    val resolvedOutputDir = outputDir ?: return CliCommand.Error("Missing required --out <output-dir> option.")
    return CliCommand.Run(script = script, outputDir = resolvedOutputDir)
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
