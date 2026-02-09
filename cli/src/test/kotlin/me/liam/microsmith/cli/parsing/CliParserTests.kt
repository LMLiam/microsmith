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
                    outputDir = Path("build/generated")
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
    })
