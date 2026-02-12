package me.liam.microsmith.cli.parsing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.cli.command.DoctorCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import me.liam.microsmith.runtime.scripting.model.ScriptIsolationMode
import kotlin.io.path.Path

class CliParserTests :
    StringSpec({
        "returns help command for empty args" {
            parseCliArgs(emptyList()) shouldBe HelpCommand
        }

        "parses doctor command with defaults" {
            parseCliArgs(listOf("doctor")) shouldBe DoctorCommand()
        }

        "parses doctor command options" {
            parseCliArgs(listOf("doctor", "--diagnostics", "json", "--verbose")) shouldBe
                DoctorCommand(
                    diagnosticsFormat = DiagnosticFormat.JSON,
                    verbose = true,
                )
        }

        "parses run command with required out option" {
            parseCliArgs(listOf("run", "schema.microsmith.kts", "--out", "build/generated")) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("build/generated"),
                    variables = emptyMap(),
                    flags = emptySet(),
                )
        }

        "parses run command with vars and flags" {
            parseCliArgs(
                listOf(
                    "run",
                    "schema.microsmith.kts",
                    "--out",
                    "build/generated",
                    "--var",
                    "env=prod",
                    "--var",
                    "team=platform",
                    "--flag",
                    "dry-run",
                    "--flag",
                    "verbose",
                ),
            ) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("build/generated"),
                    variables = mapOf("env" to "prod", "team" to "platform"),
                    flags = setOf("dry-run", "verbose"),
                )
        }

        "parses run command with plugin resolution options" {
            parseCliArgs(
                listOf(
                    "run",
                    "schema.microsmith.kts",
                    "--out",
                    "build/generated",
                    "--plugin",
                    "com.acme:microsmith-emitter-ts:1.4.2",
                    "--plugin-jar",
                    "plugins/custom.jar",
                    "--offline",
                    "--repository",
                    "https://maven.acme.internal/repository/mirror",
                    "--isolation",
                    "process",
                ),
            ) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("build/generated"),
                    plugins = setOf("com.acme:microsmith-emitter-ts:1.4.2"),
                    pluginJars = setOf(Path("plugins/custom.jar")),
                    offline = true,
                    repositoryOverride = "https://maven.acme.internal/repository/mirror",
                    isolationMode = ScriptIsolationMode.PROCESS,
                )
        }

        "parses run command diagnostics, verbose mode, and audit log options" {
            parseCliArgs(
                listOf(
                    "run",
                    "schema.microsmith.kts",
                    "--out",
                    "build/generated",
                    "--diagnostics",
                    "json",
                    "--verbose",
                    "--audit-log",
                    "build/audit.jsonl",
                ),
            ) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("build/generated"),
                    diagnosticsFormat = DiagnosticFormat.JSON,
                    verbose = true,
                    auditLog = Path("build/audit.jsonl"),
                )
        }

        "returns error when run command has missing out option" {
            parseCliArgs(listOf("run", "schema.microsmith.kts")) shouldBe
                ErrorCommand("Missing required --out <output-dir> option.")
        }

        "returns error when script has unsupported extension" {
            parseCliArgs(listOf("run", "schema.ms.kts", "--out", "build/generated")) shouldBe
                ErrorCommand("Script file must use the .microsmith.kts extension.")
        }

        "returns error for unknown option" {
            parseCliArgs(listOf("run", "schema.microsmith.kts", "--bad", "value")) shouldBe
                ErrorCommand("Unknown option '--bad'.")
        }

        "returns error for invalid --var value" {
            parseCliArgs(
                listOf("run", "schema.microsmith.kts", "--out", "build/generated", "--var", "broken"),
            ) shouldBe
                ErrorCommand("Invalid --var value 'broken'. Expected key=value.")
        }

        "returns error for malformed --plugin coordinate" {
            parseCliArgs(
                listOf(
                    "run",
                    "schema.microsmith.kts",
                    "--out",
                    "build/generated",
                    "--plugin",
                    "com.acme:missing-version",
                ),
            ) shouldBe
                ErrorCommand(
                    "Invalid --plugin value 'com.acme:missing-version'. Expected group:artifact:version.",
                )
        }

        "returns error for invalid isolation mode" {
            parseCliArgs(
                listOf(
                    "run",
                    "schema.microsmith.kts",
                    "--out",
                    "build/generated",
                    "--isolation",
                    "container",
                ),
            ) shouldBe
                ErrorCommand("Invalid --isolation value 'container'. Expected 'classloader' or 'process'.")
        }

        "returns error for invalid diagnostics mode" {
            parseCliArgs(
                listOf(
                    "run",
                    "schema.microsmith.kts",
                    "--out",
                    "build/generated",
                    "--diagnostics",
                    "yaml",
                ),
            ) shouldBe
                ErrorCommand("Invalid --diagnostics value 'yaml'. Expected 'text' or 'json'.")
        }

        "returns error for unknown doctor option" {
            parseCliArgs(listOf("doctor", "--audit-log", "build/audit.jsonl")) shouldBe
                ErrorCommand("Unknown option '--audit-log'.")
        }
    })
