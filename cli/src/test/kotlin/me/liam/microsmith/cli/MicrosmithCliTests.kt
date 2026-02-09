package me.liam.microsmith.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.parsing.parseCliArgs
import java.util.ServiceConfigurationError
import kotlin.io.path.Path

class MicrosmithCliTests :
    StringSpec({
        "returns help for empty args" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli = MicrosmithCli(stdout = out::add, stderr = err::add)

            val exitCode = cli.run(emptyArray())

            exitCode shouldBe 0
            out.joinToString("\n").shouldContain("Usage:")
            err shouldBe emptyList()
        }

        "returns error for unknown command" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli = MicrosmithCli(stdout = out::add, stderr = err::add)

            val exitCode = cli.run(arrayOf("unknown"))

            exitCode shouldBe 2
            err.joinToString("\n").shouldContain("Unknown command")
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

        "returns error when provider validation fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { listOf("missing providers") }
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 2
            err.shouldContain("missing providers")
        }

        "returns structured error when service provider loading fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { throw ServiceConfigurationError("bad provider entry") }
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 2
            err.joinToString("\n").shouldContain("Failed to load runtime service providers")
            err.joinToString("\n").shouldContain("bad provider entry")
        }

        "returns success for run command when providers are available" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { emptyList() }
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 0
            out.joinToString("\n").shouldContain("Phase 1 scaffold complete")
            err shouldBe emptyList()
        }
    })
