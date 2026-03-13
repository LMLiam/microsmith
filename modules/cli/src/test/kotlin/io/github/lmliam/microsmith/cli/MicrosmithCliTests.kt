package io.github.lmliam.microsmith.cli

import io.github.lmliam.microsmith.cli.command.IdeDoctorCommand
import io.github.lmliam.microsmith.cli.command.IdeRefreshCommand
import io.github.lmliam.microsmith.cli.doctor.DoctorCheckResult
import io.github.lmliam.microsmith.cli.doctor.DoctorCheckStatus
import io.github.lmliam.microsmith.cli.doctor.DoctorResult
import io.github.lmliam.microsmith.cli.ide.IdeDoctorCheckResult
import io.github.lmliam.microsmith.cli.ide.IdeDoctorResult
import io.github.lmliam.microsmith.cli.ide.IdeHelperRefreshResult
import io.github.lmliam.microsmith.cli.plugins.PluginResolutionResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

        "returns version string for --version command" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    versionProvider = { "9.9.9-test" },
                )

            val exitCode = cli.run(arrayOf("--version"))

            exitCode shouldBe 0
            out shouldBe listOf("microsmith 9.9.9-test")
            err shouldBe emptyList()
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

        "returns deterministic plugin resolution exit code when plugin resolver throws unexpectedly" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    providerValidator = { emptyList() },
                    pluginResolver = {
                        throw IllegalStateException("simulated resolver crash")
                    },
                )

            val exitCode = cli.run(arrayOf("run", "schema.microsmith.kts", "--out", "build/generated"))

            exitCode shouldBe 11
            err.joinToString("\n").shouldContain("Plugin resolution failed unexpectedly")
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

        "ide refresh command returns success when helper generation succeeds" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-ide-refresh")
            try {
                val helperRoot = tempDir.resolve(".microsmith/ide")
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        ideRefreshRunner = { command: IdeRefreshCommand ->
                            IdeHelperRefreshResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                helperRoot = helperRoot,
                                updatedFiles = listOf(helperRoot.resolve("build.gradle.kts")),
                                classpathEntries = listOf(tempDir.resolve("microsmith-cli-all.jar")),
                            )
                        },
                    )

                val exitCode = cli.run(arrayOf("ide", "refresh", "--repo-root", tempDir.toString()))

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("JetBrains IDE helper is updated")
                out.joinToString("\n").shouldContain("Import")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "ide refresh command returns deterministic failure code when helper generation fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    ideRefreshRunner = {
                        error("simulated helper generation failure")
                    },
                )

            val exitCode = cli.run(arrayOf("ide", "refresh"))

            exitCode shouldBe 40
            err.joinToString("\n").shouldContain("MS-CLI-4001")
            err.joinToString("\n").shouldContain("simulated helper generation failure")
            out shouldBe emptyList()
        }

        "ide doctor command returns success when checks pass" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-ide-doctor-success")
            try {
                val helperRoot = tempDir.resolve(".microsmith/ide")
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        ideDoctorRunner = { command: IdeDoctorCommand ->
                            IdeDoctorResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                helperRoot = helperRoot,
                                checks =
                                listOf(
                                    IdeDoctorCheckResult(
                                        id = "helper-directory",
                                        passed = true,
                                        message = "IDE helper directory exists.",
                                    ),
                                ),
                            )
                        },
                    )

                val exitCode = cli.run(arrayOf("ide", "doctor", "--repo-root", tempDir.toString()))

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("JetBrains IDE helper doctor checks passed")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "ide doctor command returns deterministic failure code when checks fail" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val projectRoot = createTempDirectory("microsmith-cli-ide-doctor-failure")
            val helperRoot = createTempDirectory("microsmith-cli-ide-doctor-helper")
            try {
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        ideDoctorRunner = {
                            IdeDoctorResult(
                                projectRoot = projectRoot,
                                helperRoot = helperRoot,
                                checks =
                                listOf(
                                    IdeDoctorCheckResult(
                                        id = "classpath-sync",
                                        passed = false,
                                        message = "IDE helper build file is stale.",
                                    ),
                                ),
                            )
                        },
                    )

                val exitCode = cli.run(arrayOf("ide", "doctor"))

                exitCode shouldBe 41
                err.joinToString("\n").shouldContain("MS-CLI-4101")
                err.joinToString("\n").shouldContain("JetBrains IDE helper doctor detected issues")
                out shouldBe emptyList()
            } finally {
                runCatching { projectRoot.deleteRecursively() }
                runCatching { helperRoot.deleteRecursively() }
            }
        }

        "ide doctor command emits json error diagnostics payload when checks fail" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-ide-doctor-json-failure")
            try {
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        ideDoctorRunner = { command ->
                            IdeDoctorResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                helperRoot = tempDir.resolve(".microsmith/ide"),
                                checks =
                                listOf(
                                    IdeDoctorCheckResult(
                                        id = "classpath-sync",
                                        passed = false,
                                        message = "IDE helper build file is stale.",
                                    ),
                                ),
                            )
                        },
                    )

                val exitCode =
                    cli.run(
                        arrayOf(
                            "ide",
                            "doctor",
                            "--repo-root",
                            tempDir.toString(),
                            "--diagnostics",
                            "json",
                        ),
                    )

                exitCode shouldBe 41
                err.joinToString("\n").shouldContain("\"code\":\"MS-CLI-4101\"")
                err.joinToString("\n").shouldContain("\"level\":\"error\"")
                out shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
