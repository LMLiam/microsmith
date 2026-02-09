package me.liam.microsmith.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.runtime.scripting.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.ScriptRunSuccess
import java.util.ServiceConfigurationError

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
                    providerValidator = { emptyList() },
                    scriptRunner = {
                        ScriptRunSuccess(
                            warnings = emptyList(),
                            cacheHit = false,
                            elapsedMillis = 12
                        )
                    }
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 0
            out.joinToString("\n").shouldContain("Generated script")
            err shouldBe emptyList()
        }

        "returns error when script execution fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { emptyList() },
                    scriptRunner = { ScriptRunFailure(listOf("[error] schema.microsmith.kts:1:1 broken script")) }
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 2
            err.joinToString("\n").shouldContain("broken script")
            out shouldBe emptyList()
        }
    })
