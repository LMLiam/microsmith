package me.liam.microsmith.cli.parsing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import kotlin.io.path.Path

class CliParserTests :
    StringSpec({
        "returns help command for empty args" {
            parseCliArgs(emptyList()) shouldBe HelpCommand
        }

        "parses run command with required out option" {
            parseCliArgs(listOf("run", "schema.microsmith.kts", "--out", "build/generated")) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("build/generated"),
                    variables = emptyMap(),
                    flags = emptySet()
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
                    "verbose"
                )
            ) shouldBe
                RunCommand(
                    script = Path("schema.microsmith.kts"),
                    outputDir = Path("build/generated"),
                    variables = mapOf("env" to "prod", "team" to "platform"),
                    flags = setOf("dry-run", "verbose")
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
                listOf("run", "schema.microsmith.kts", "--out", "build/generated", "--var", "broken")
            ) shouldBe
                ErrorCommand("Invalid --var value 'broken'. Expected key=value.")
        }
    })
