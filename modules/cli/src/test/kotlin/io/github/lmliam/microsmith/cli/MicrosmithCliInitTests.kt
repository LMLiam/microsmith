package io.github.lmliam.microsmith.cli

import io.github.lmliam.microsmith.cli.command.InitCommand
import io.github.lmliam.microsmith.cli.ide.IdeHelperRefreshResult
import io.github.lmliam.microsmith.cli.init.GenericOnboardingProfile
import io.github.lmliam.microsmith.cli.init.GoOnboardingProfile
import io.github.lmliam.microsmith.cli.init.InitBootstrapResult
import io.github.lmliam.microsmith.cli.init.InitConflictException
import io.github.lmliam.microsmith.cli.init.InitValidationException
import io.github.lmliam.microsmith.cli.init.NodeOnboardingProfile
import io.github.lmliam.microsmith.cli.init.OnboardingProfileDetection
import io.github.lmliam.microsmith.cli.init.OnboardingProfileSelectionReason
import io.github.lmliam.microsmith.cli.init.PythonOnboardingProfile
import io.github.lmliam.microsmith.cli.init.RubyOnboardingProfile
import io.github.lmliam.microsmith.cli.init.RustOnboardingProfile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class MicrosmithCliInitTests :
    StringSpec({
        "init command returns success and emits next run command when bootstrap succeeds" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-init-success")
            try {
                val helperRoot = tempDir.resolve(".microsmith/ide")
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        initRunner = { command: InitCommand ->
                            InitBootstrapResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                repositoryDetection =
                                OnboardingProfileDetection(
                                    profile = GoOnboardingProfile,
                                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                                    matchedMarkers = listOf("go.mod"),
                                ),
                                createdFiles =
                                listOf(
                                    tempDir.resolve("build.microsmith.kts"),
                                    tempDir.resolve("settings.microsmith.kts"),
                                ),
                                overwrittenFiles = emptyList(),
                                preservedFiles = emptyList(),
                                ideHelperResult =
                                IdeHelperRefreshResult(
                                    projectRoot = tempDir,
                                    helperRoot = helperRoot,
                                    updatedFiles = listOf(helperRoot.resolve("build.gradle.kts")),
                                    classpathEntries = listOf(tempDir.resolve("microsmith-cli-all.jar")),
                                ),
                            )
                        },
                    )

                val exitCode = cli.run(arrayOf("init", "--repo-root", tempDir.toString()))

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("Microsmith init completed")
                out.joinToString("\n").shouldContain("Detected repository profile: Go")
                out.joinToString("\n").shouldContain("build.microsmith.kts")
                out.joinToString("\n").shouldContain("JetBrains IDE helper is updated")
                out.joinToString("\n").shouldContain("microsmith run build.microsmith.kts --out ./generated")
                out.joinToString("\n").shouldContain("./internal/gen")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "init command returns deterministic conflict failure code when bootstrap detects conflicting path" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    initRunner = {
                        throw InitConflictException("Bootstrap path is not a regular file.")
                    },
                )

            val exitCode = cli.run(arrayOf("init"))

            exitCode shouldBe 50
            err.joinToString("\n").shouldContain("MS-CLI-5001")
            err.joinToString("\n").shouldContain("Bootstrap path is not a regular file")
            out shouldBe emptyList()
        }

        "init command returns deterministic validation failure code when bootstrap validation fails" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    initRunner = {
                        throw InitValidationException("Repository root does not exist.")
                    },
                )

            val exitCode = cli.run(arrayOf("init", "--repo-root", "/path/does/not/exist"))

            exitCode shouldBe 51
            err.joinToString("\n").shouldContain("MS-CLI-5002")
            err.joinToString("\n").shouldContain("Repository root does not exist.")
            out shouldBe emptyList()
        }

        "init command treats unexpected illegal arguments as runtime failures" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    initRunner = {
                        throw IllegalArgumentException("IDE helper refresh failed.")
                    },
                )

            val exitCode = cli.run(arrayOf("init"))

            exitCode shouldBe 52
            err.joinToString("\n").shouldContain("MS-CLI-5003")
            err.joinToString("\n").shouldContain("IDE helper refresh failed.")
            out shouldBe emptyList()
        }

        "init command returns deterministic runtime failure code for unexpected bootstrap errors" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val cli =
                MicrosmithCli(
                    stdout = out::add,
                    stderr = err::add,
                    initRunner = {
                        throw IllegalStateException("Unexpected init failure.")
                    },
                )

            val exitCode = cli.run(arrayOf("init"))

            exitCode shouldBe 52
            err.joinToString("\n").shouldContain("MS-CLI-5003")
            err.joinToString("\n").shouldContain("Unexpected init failure.")
            out shouldBe emptyList()
        }

        "init command emits json diagnostics payload when requested" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-init-json")
            try {
                val helperRoot = tempDir.resolve(".microsmith/ide")
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        initRunner = { command: InitCommand ->
                            InitBootstrapResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                repositoryDetection =
                                OnboardingProfileDetection(
                                    profile = NodeOnboardingProfile,
                                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                                    matchedMarkers = listOf("package.json"),
                                ),
                                createdFiles = listOf(tempDir.resolve("build.microsmith.kts")),
                                overwrittenFiles = emptyList(),
                                preservedFiles = listOf(tempDir.resolve("settings.microsmith.kts")),
                                ideHelperResult =
                                IdeHelperRefreshResult(
                                    projectRoot = tempDir,
                                    helperRoot = helperRoot,
                                    updatedFiles = listOf(helperRoot.resolve("build.gradle.kts")),
                                    classpathEntries = listOf(tempDir.resolve("microsmith-cli-all.jar")),
                                ),
                            )
                        },
                    )

                val exitCode =
                    cli.run(
                        arrayOf(
                            "init",
                            "--repo-root",
                            tempDir.toString(),
                            "--diagnostics",
                            "json",
                            "--verbose",
                        ),
                    )

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("\"level\":\"info\"")
                out.joinToString("\n").shouldContain("Microsmith init completed")
                out.joinToString("\n").shouldContain("Detected repository profile: Node")
                out.joinToString("\n").shouldContain("\"details\"")
                out.joinToString("\n").shouldContain("\"repositoryProfile\":\"node\"")
                out.joinToString("\n").shouldContain("\"repositoryProfileDisplayName\":\"Node\"")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "init command reports preserved files and skipped IDE helper when requested" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-init-skip-ide")
            try {
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        initRunner = { command: InitCommand ->
                            command.skipIdeHelper shouldBe true
                            InitBootstrapResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                repositoryDetection =
                                OnboardingProfileDetection(
                                    profile = GenericOnboardingProfile,
                                    selectionReason = OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                                    matchedMarkers = emptyList(),
                                ),
                                createdFiles = emptyList(),
                                overwrittenFiles = emptyList(),
                                preservedFiles = listOf(tempDir.resolve("build.microsmith.kts")),
                                ideHelperResult = null,
                            )
                        },
                    )

                val exitCode =
                    cli.run(
                        arrayOf(
                            "init",
                            "--skip-ide-helper",
                            "--repo-root",
                            tempDir.toString(),
                        ),
                    )

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("Preserved existing bootstrap files")
                out.joinToString("\n").shouldContain("Re-run with --force")
                out.joinToString("\n").shouldContain("JetBrains IDE helper generation was skipped")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "init command keeps Python on the canonical generated output path" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-init-python")
            try {
                val helperRoot = tempDir.resolve(".microsmith/ide")
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        initRunner = { command: InitCommand ->
                            InitBootstrapResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                repositoryDetection =
                                OnboardingProfileDetection(
                                    profile = PythonOnboardingProfile,
                                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                                    matchedMarkers = listOf("pyproject.toml"),
                                ),
                                createdFiles = listOf(tempDir.resolve("build.microsmith.kts")),
                                overwrittenFiles = emptyList(),
                                preservedFiles = emptyList(),
                                ideHelperResult =
                                IdeHelperRefreshResult(
                                    projectRoot = tempDir,
                                    helperRoot = helperRoot,
                                    updatedFiles = emptyList(),
                                    classpathEntries = listOf(tempDir.resolve("microsmith-cli-all.jar")),
                                ),
                            )
                        },
                    )

                val exitCode = cli.run(arrayOf("init", "--repo-root", tempDir.toString()))

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("Detected repository profile: Python")
                out.joinToString("\n").shouldContain("Next: microsmith run build.microsmith.kts --out ./generated")
                out.joinToString("\n").shouldNotContain("Optional repository-native output path")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "init command keeps Ruby on the canonical generated output path" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-init-ruby")
            try {
                val helperRoot = tempDir.resolve(".microsmith/ide")
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        initRunner = { command: InitCommand ->
                            InitBootstrapResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                repositoryDetection =
                                OnboardingProfileDetection(
                                    profile = RubyOnboardingProfile,
                                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                                    matchedMarkers = listOf("Gemfile"),
                                ),
                                createdFiles = listOf(tempDir.resolve("build.microsmith.kts")),
                                overwrittenFiles = emptyList(),
                                preservedFiles = emptyList(),
                                ideHelperResult =
                                IdeHelperRefreshResult(
                                    projectRoot = tempDir,
                                    helperRoot = helperRoot,
                                    updatedFiles = emptyList(),
                                    classpathEntries = listOf(tempDir.resolve("microsmith-cli-all.jar")),
                                ),
                            )
                        },
                    )

                val exitCode = cli.run(arrayOf("init", "--repo-root", tempDir.toString()))

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("Detected repository profile: Ruby")
                out.joinToString("\n").shouldContain("Next: microsmith run build.microsmith.kts --out ./generated")
                out.joinToString("\n").shouldNotContain("Optional repository-native output path")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "init command keeps Rust on the canonical generated output path" {
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val tempDir = createTempDirectory("microsmith-cli-init-rust")
            try {
                val helperRoot = tempDir.resolve(".microsmith/ide")
                val cli =
                    MicrosmithCli(
                        stdout = out::add,
                        stderr = err::add,
                        initRunner = { command: InitCommand ->
                            InitBootstrapResult(
                                projectRoot = command.projectRoot.toAbsolutePath().normalize(),
                                repositoryDetection =
                                OnboardingProfileDetection(
                                    profile = RustOnboardingProfile,
                                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                                    matchedMarkers = listOf("Cargo.toml"),
                                ),
                                createdFiles = listOf(tempDir.resolve("build.microsmith.kts")),
                                overwrittenFiles = emptyList(),
                                preservedFiles = emptyList(),
                                ideHelperResult =
                                IdeHelperRefreshResult(
                                    projectRoot = tempDir,
                                    helperRoot = helperRoot,
                                    updatedFiles = emptyList(),
                                    classpathEntries = listOf(tempDir.resolve("microsmith-cli-all.jar")),
                                ),
                            )
                        },
                    )

                val exitCode = cli.run(arrayOf("init", "--repo-root", tempDir.toString()))

                exitCode shouldBe 0
                out.joinToString("\n").shouldContain("Detected repository profile: Rust")
                out.joinToString("\n").shouldContain("Next: microsmith run build.microsmith.kts --out ./generated")
                out.joinToString("\n").shouldNotContain("Optional repository-native output path")
                err shouldBe emptyList()
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
