package me.liam.microsmith.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.cli.doctor.DoctorCheckResult
import me.liam.microsmith.cli.doctor.DoctorCheckStatus
import me.liam.microsmith.cli.doctor.DoctorResult
import me.liam.microsmith.cli.plugins.PluginResolutionResult
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.util.ServiceConfigurationError
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
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
            err.joinToString("\n").shouldContain("MS-CLI-0001")
        }

        "returns provider validation exit code when provider validation fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { listOf("missing providers") },
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 10
            err.joinToString("\n").shouldContain("missing providers")
            err.joinToString("\n").shouldContain("MS-CLI-1001")
        }

        "returns provider validation exit code when service provider loading fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { throw ServiceConfigurationError("bad provider entry") },
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 10
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
                    scriptRunner = { _, _ ->
                        ScriptRunSuccess(
                            warnings = emptyList(),
                            cacheHit = false,
                            elapsedMillis = 12,
                        )
                    },
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 0
            out.joinToString("\n").shouldContain("Generated script")
            err shouldBe emptyList()
        }

        "returns deterministic script compilation exit code when script execution fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { emptyList() },
                    scriptRunner = { _, _ ->
                        ScriptRunFailure(
                            diagnostics = listOf("[error] schema.microsmith.kts:1:1 broken script"),
                            type = ScriptFailureType.COMPILATION,
                        )
                    },
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 21
            err.joinToString("\n").shouldContain("broken script")
            err.joinToString("\n").shouldContain("MS-CLI-2002")
            out shouldBe emptyList()
        }

        "returns deterministic plugin resolution exit code when plugin resolution fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { emptyList() },
                    pluginResolver = {
                        PluginResolutionResult.Failure(
                            diagnostics = listOf("Failed to resolve plugin from repository mirror."),
                        )
                    },
                )

            val exitCode =
                cli.run(
                    arrayOf(
                        "run",
                        "schema.microsmith.kts",
                        "--out",
                        "build/generated",
                        "--plugin",
                        "com.acme:test-emitter:1.0.0",
                    ),
                )

            exitCode shouldBe 11
            err.joinToString("\n").shouldContain("Failed to resolve plugin from repository mirror.")
            err.joinToString("\n").shouldContain("MS-CLI-1101")
            out shouldBe emptyList()
        }

        "emits machine readable diagnostics when json mode is requested" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { emptyList() },
                    pluginResolver = {
                        PluginResolutionResult.Failure(
                            diagnostics = listOf("Plugin repository policy blocked endpoint."),
                        )
                    },
                )

            val exitCode =
                cli.run(
                    arrayOf(
                        "run",
                        "schema.microsmith.kts",
                        "--out",
                        "build/generated",
                        "--diagnostics",
                        "json",
                    ),
                )

            exitCode shouldBe 11
            err.joinToString("\n").shouldContain("\"code\":\"MS-CLI-1101\"")
            err.joinToString("\n").shouldContain("\"level\":\"error\"")
        }

        "writes event log entry for successful run when event log path is configured" {
            val tempDir = createTempDirectory("microsmith-cli-event-log-success")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val outputDir = tempDir.resolve("generated")
                val eventLogPath = tempDir.resolve("logs/event-log.jsonl")
                script.writeText("microsmith { }")

                val cli =
                    MicrosmithCli(
                        providerValidator = { emptyList() },
                        scriptRunner = { _, _ ->
                            ScriptRunSuccess(
                                warnings = emptyList(),
                                cacheHit = true,
                                elapsedMillis = 7,
                            )
                        },
                    )

                val exitCode =
                    cli.run(
                        arrayOf(
                            "run",
                            script.toString(),
                            "--out",
                            outputDir.toString(),
                            "--event-log",
                            eventLogPath.toString(),
                        ),
                    )

                exitCode shouldBe 0
                eventLogPath.exists() shouldBe true
                val eventLogLine = eventLogPath.readLines().single()
                eventLogLine.shouldContain("\"event\":\"microsmith.run\"")
                eventLogLine.shouldContain("\"status\":\"success\"")
                eventLogLine.shouldContain("\"cacheHit\":true")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "doctor command returns success when checks pass" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    doctorRunner = {
                        DoctorResult(
                            checks =
                            listOf(
                                DoctorCheckResult(
                                    id = "provider-discovery",
                                    status = DoctorCheckStatus.PASS,
                                    message = "Required providers available.",
                                ),
                            ),
                        )
                    },
                )

            val exitCode = cli.run(arrayOf("doctor"))

            exitCode shouldBe 0
            out.joinToString("\n").shouldContain("Doctor checks passed")
            err shouldBe emptyList()
        }

        "doctor command returns deterministic failure exit code when checks fail" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    doctorRunner = {
                        DoctorResult(
                            checks =
                            listOf(
                                DoctorCheckResult(
                                    id = "provider-discovery",
                                    status = DoctorCheckStatus.FAIL,
                                    message = "Provider loading failed.",
                                ),
                            ),
                        )
                    },
                )

            val exitCode = cli.run(arrayOf("doctor"))

            exitCode shouldBe 30
            err.joinToString("\n").shouldContain("MS-CLI-3001")
            err.joinToString("\n").shouldContain("Doctor detected environment issues.")
            out shouldBe emptyList()
        }
    })
