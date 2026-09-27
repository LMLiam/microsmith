package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.command.DoctorCommand
import io.github.lmliam.microsmith.cli.command.ErrorCommand
import io.github.lmliam.microsmith.cli.command.HelpCommand
import io.github.lmliam.microsmith.cli.command.IdeDoctorCommand
import io.github.lmliam.microsmith.cli.command.IdeRefreshCommand
import io.github.lmliam.microsmith.cli.command.InitCommand
import io.github.lmliam.microsmith.cli.command.RunCommand
import io.github.lmliam.microsmith.cli.command.VersionCommand
import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.Path

class CliParserTests :
    StringSpec({
        "returns help command for empty args" {
            parseCliArgs(emptyList()) shouldBe HelpCommand
        }

        "parses --version command" {
            parseCliArgs(listOf("--version")) shouldBe VersionCommand
        }

        "returns error for version alias command" {
            parseCliArgs(listOf("version")) shouldBe ErrorCommand("Unknown command 'version'.")
        }

        "returns error when --version has extra arguments" {
            parseCliArgs(listOf("--version", "--verbose")) shouldBe
                ErrorCommand("The --version command does not accept additional arguments.")
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

        "parses init command with defaults" {
            parseCliArgs(listOf("init")) shouldBe InitCommand()
        }

        "parses init command options" {
            parseCliArgs(
                listOf(
                    "init",
                    "--repo-root",
                    "examples/go-service",
                    "--force",
                    "--skip-ide-helper",
                    "--diagnostics",
                    "json",
                    "--verbose",
                ),
            ) shouldBe
                InitCommand(
                    projectRoot = Path("examples/go-service"),
                    diagnosticsFormat = DiagnosticFormat.JSON,
                    verbose = true,
                    force = true,
                    skipIdeHelper = true,
                )
        }

        "parses ide refresh command with defaults" {
            parseCliArgs(listOf("ide", "refresh")) shouldBe IdeRefreshCommand()
        }

        "parses ide refresh command options" {
            parseCliArgs(
                listOf(
                    "ide",
                    "refresh",
                    "--repo-root",
                    "examples/go-service",
                    "--diagnostics",
                    "json",
                    "--verbose",
                ),
            ) shouldBe
                IdeRefreshCommand(
                    projectRoot = Path("examples/go-service"),
                    diagnosticsFormat = DiagnosticFormat.JSON,
                    verbose = true,
                )
        }

        "parses ide doctor command options" {
            parseCliArgs(
                listOf(
                    "ide",
                    "doctor",
                    "--repo-root",
                    "examples/dotnet-service",
                    "--diagnostics",
                    "json",
                    "--verbose",
                ),
            ) shouldBe
                IdeDoctorCommand(
                    projectRoot = Path("examples/dotnet-service"),
                    diagnosticsFormat = DiagnosticFormat.JSON,
                    verbose = true,
                )
        }

        "parses run command with default output root when --out is omitted" {
            parseCliArgs(listOf("run", "schema.microsmith.kts")) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("."),
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

        "parses run command diagnostics, verbose mode, and event log options" {
            parseCliArgs(
                listOf(
                    "run",
                    "schema.microsmith.kts",
                    "--out",
                    "build/generated",
                    "--diagnostics",
                    "json",
                    "--verbose",
                    "--event-log",
                    "build/event-log.jsonl",
                ),
            ) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("build/generated"),
                    diagnosticsFormat = DiagnosticFormat.JSON,
                    verbose = true,
                    eventLog = Path("build/event-log.jsonl"),
                )
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

        "returns error when ide subcommand is missing" {
            parseCliArgs(listOf("ide")) shouldBe ErrorCommand("Missing <refresh|doctor> subcommand for ide command.")
        }

        "returns error for unknown ide subcommand" {
            parseCliArgs(listOf("ide", "lint")) shouldBe
                ErrorCommand("Unknown ide subcommand 'lint'. Expected 'refresh' or 'doctor'.")
        }

        "returns error for unknown ide option" {
            parseCliArgs(listOf("ide", "refresh", "--event-log", "build/event-log.jsonl")) shouldBe
                ErrorCommand("Unknown option '--event-log'.")
        }

        "returns error when ide repo-root value is missing" {
            parseCliArgs(listOf("ide", "refresh", "--repo-root")) shouldBe
                ErrorCommand("Missing value for --repo-root option.")
        }

        "returns error when ide repo-root is specified multiple times" {
            parseCliArgs(
                listOf(
                    "ide",
                    "refresh",
                    "--repo-root",
                    "repo-one",
                    "--repo-root",
                    "repo-two",
                ),
            ) shouldBe ErrorCommand("--repo-root may only be specified once.")
        }

        "returns error when ide diagnostics format is invalid" {
            parseCliArgs(listOf("ide", "refresh", "--diagnostics", "yaml")) shouldBe
                ErrorCommand("Invalid --diagnostics value 'yaml'. Expected 'text' or 'json'.")
        }

        "returns error when ide verbose is specified multiple times" {
            parseCliArgs(listOf("ide", "refresh", "--verbose", "--verbose")) shouldBe
                ErrorCommand("--verbose may only be specified once.")
        }

        "returns error for unknown ide doctor option" {
            parseCliArgs(listOf("ide", "doctor", "--event-log", "build/event-log.jsonl")) shouldBe
                ErrorCommand("Unknown option '--event-log'.")
        }

        "returns error for unknown init option" {
            parseCliArgs(listOf("init", "--event-log", "build/event-log.jsonl")) shouldBe
                ErrorCommand("Unknown option '--event-log'.")
        }

        "returns error when init diagnostics format is invalid" {
            parseCliArgs(listOf("init", "--diagnostics", "yaml")) shouldBe
                ErrorCommand("Invalid --diagnostics value 'yaml'. Expected 'text' or 'json'.")
        }

        "returns error when init repo-root value is missing" {
            parseCliArgs(listOf("init", "--repo-root")) shouldBe
                ErrorCommand("Missing value for --repo-root option.")
        }

        "returns error when init repo-root is specified multiple times" {
            parseCliArgs(
                listOf("init", "--repo-root", "one", "--repo-root", "two"),
            ) shouldBe ErrorCommand("--repo-root may only be specified once.")
        }

        "returns error for removed init non-interactive option" {
            parseCliArgs(listOf("init", "--non-interactive")) shouldBe
                ErrorCommand("Unknown option '--non-interactive'.")
        }

        "returns error for removed init yes option" {
            parseCliArgs(listOf("init", "--yes")) shouldBe
                ErrorCommand("Unknown option '--yes'.")
        }

        "returns error when init force is specified multiple times" {
            parseCliArgs(listOf("init", "--force", "--force")) shouldBe
                ErrorCommand("--force may only be specified once.")
        }

        "returns error when init skip-ide-helper is specified multiple times" {
            parseCliArgs(listOf("init", "--skip-ide-helper", "--skip-ide-helper")) shouldBe
                ErrorCommand("--skip-ide-helper may only be specified once.")
        }

        "returns error for unknown doctor option" {
            parseCliArgs(listOf("doctor", "--event-log", "build/event-log.jsonl")) shouldBe
                ErrorCommand("Unknown option '--event-log'.")
        }
    })
