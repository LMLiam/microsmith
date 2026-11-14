package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class MicrosmithScriptHostTests :
    StringSpec(
        {
            "runs script returning MicrosmithModel and generates protobuf output" {
                val tempDir = createTempDirectory("microsmith-script-host")
                try {
                    val script = tempDir.resolve("schema.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        microsmith {
                            schemas {
                                protobuf {
                                    "pkg" {
                                        message("User") {
                                            int32("id") { index(1) }
                                        }
                                    }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)

                    val firstRun =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )
                    val firstSuccess = firstRun.shouldBeTypeOf<ScriptRunSuccess>()
                    firstSuccess.cacheHit shouldBe false

                    val generatedFile = output.resolve("proto/pkg/User.proto")
                    generatedFile.exists() shouldBe true
                    generatedFile.readText().shouldContain("message User")

                    val secondRun =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )
                    val secondSuccess = secondRun.shouldBeTypeOf<ScriptRunSuccess>()
                    secondSuccess.cacheHit shouldBe true
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "reports script compiler warnings on a compile cache hit" {
                val tempDir = createTempDirectory("microsmith-script-host-warnings")
                try {
                    val script = tempDir.resolve("service.microsmith.kts")
                    script.writeText(
                        """
                        microsmith {
                            services {
                                dotnet {
                                    target(NET8)
                                    solutions { "Platform" {} }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )
                    val host = MicrosmithScriptHost(cacheDirectory = tempDir.resolve("cache"))
                    val request =
                        ScriptRunRequest(
                            script = script,
                            outputDir = tempDir.resolve("generated"),
                            variables = emptyMap(),
                            flags = emptySet(),
                        )

                    val firstRun = host.run(request).shouldBeTypeOf<ScriptRunSuccess>()
                    val secondRun = host.run(request).shouldBeTypeOf<ScriptRunSuccess>()

                    firstRun.cacheHit shouldBe false
                    firstRun.warnings.single().shouldContain("NET8").shouldContain("deprecated")
                    secondRun.cacheHit shouldBe true
                    secondRun.warnings shouldContainExactly firstRun.warnings
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "recompiles and reports warnings when the cached warnings file is missing or corrupt" {
                val tempDir = createTempDirectory("microsmith-script-host-warnings-damaged")
                try {
                    val script = tempDir.resolve("service.microsmith.kts")
                    script.writeText(
                        """
                        microsmith {
                            services {
                                dotnet {
                                    target(NET8)
                                    solutions { "Platform" {} }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )
                    val cache = tempDir.resolve("cache")
                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val request =
                        ScriptRunRequest(
                            script = script,
                            outputDir = tempDir.resolve("generated"),
                            variables = emptyMap(),
                            flags = emptySet(),
                        )
                    val expectedWarnings = host.run(request).shouldBeTypeOf<ScriptRunSuccess>().warnings
                    val warningsFile = cache.listDirectoryEntries("*.warnings").single()

                    warningsFile.deleteExisting()
                    val afterMissing = host.run(request).shouldBeTypeOf<ScriptRunSuccess>()
                    warningsFile.writeText("not base64 !")
                    val afterCorrupt = host.run(request).shouldBeTypeOf<ScriptRunSuccess>()
                    val afterRepair = host.run(request).shouldBeTypeOf<ScriptRunSuccess>()

                    afterMissing.cacheHit shouldBe false
                    afterMissing.warnings shouldContainExactly expectedWarnings
                    afterCorrupt.cacheHit shouldBe false
                    afterCorrupt.warnings shouldContainExactly expectedWarnings
                    afterRepair.cacheHit shouldBe true
                    afterRepair.warnings shouldContainExactly expectedWarnings
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "runs script returning ASP NET service output with bundled .NET DSL imports" {
                val tempDir = createTempDirectory("microsmith-script-host-dotnet-asp")
                try {
                    val script = tempDir.resolve("service.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        microsmith {
                            services {
                                dotnet {
                                    target(NET10)
                                    solutions {
                                        "Platform" {}
                                    }
                                }

                                "UserService" {
                                    dotnet {
                                        solution("Platform")
                                        project("UserService.Api")
                                        models {
                                            "User" {
                                                string("id")
                                            }
                                        }

                                        asp {
                                            rest {
                                                "/users" {
                                                    get("/{id}", "GetUser") {
                                                        path("GetUserPath") {
                                                            string("id")
                                                        }

                                                        responses {
                                                            ok("User")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )

                    result.shouldBeTypeOf<ScriptRunSuccess>()
                    output.resolve("dotnet/Platform/UserService.Api/Program.cs").exists() shouldBe true
                    output
                        .resolve(
                            "dotnet/Platform/UserService.Api/Generated/Controllers/" +
                                "UserServiceApiControllerBase.cs",
                        )
                        .exists() shouldBe true
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "supports emit helper with vars and flags from CLI context" {
                val tempDir = createTempDirectory("microsmith-script-host-context")
                try {
                    val script = tempDir.resolve("context.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        val schemaName = requireVar("schema")
                        if (!hasFlag("emit")) {
                            error("emit flag is required")
                        }

                        val model =
                            microsmith {
                                schemas {
                                    protobuf {
                                        message(schemaName) {
                                            int32("id") { index(1) }
                                        }
                                    }
                                }
                            }
                        emit(model)
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = mapOf("schema" to "EventRecord"),
                                flags = setOf("emit"),
                            ),
                        )

                    result.shouldBeTypeOf<ScriptRunSuccess>()
                    val generatedFile = output.resolve("proto/EventRecord.proto")
                    Files.exists(generatedFile) shouldBe true
                    generatedFile.readText().shouldContain("message EventRecord")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "runs service scripts and generates ASP.NET outputs" {
                val tempDir = createTempDirectory("microsmith-script-host-dotnet-asp")
                try {
                    val script = tempDir.resolve("service.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        microsmith {
                            services {
                                dotnet {
                                    target(NET10)
                                    solutions {
                                        "Platform" {}
                                    }
                                }

                                "UserService" {
                                    dotnet {
                                        solution("Platform")
                                        project("UserService.Api")
                                        models {
                                            "User" {
                                                string("id")
                                                string("email")
                                            }
                                            "Problem" {
                                                string("detail")
                                            }
                                        }
                                        asp {
                                            rest {
                                                "/users" {
                                                    get("/{id}", "GetUser") {
                                                        path("GetUserPath") {
                                                            string("id")
                                                        }
                                                        responses {
                                                            ok("User")
                                                            notFound("Problem")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )

                    result.shouldBeTypeOf<ScriptRunSuccess>()
                    output.resolve("dotnet/Platform/UserService.Api/Program.cs").exists() shouldBe true
                    output
                        .resolve(
                            "dotnet/Platform/UserService.Api/Generated/Controllers/" +
                                "UserServiceApiControllerBase.cs",
                        )
                        .exists() shouldBe true
                    output
                        .resolve(
                            "dotnet/Platform/UserService.Api/Generated/Controllers/UserServiceApiControllerBase.cs",
                        )
                        .readText()
                        .shouldContain("""[HttpGet("/users/{id}", Name = "GetUser")]""")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "returns deterministic diagnostics for script errors" {
                val tempDir = createTempDirectory("microsmith-script-host-error")
                try {
                    val script = tempDir.resolve("broken.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        microsmith {
                            unknownDsl()
                        }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )

                    val failure = result.shouldBeTypeOf<ScriptRunFailure>()
                    failure.diagnostics.joinToString("\n") { it.message }.shouldContain("[error]")
                    failure.diagnostics.joinToString("\n") { it.message }.shouldContain("broken.microsmith.kts")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "blocks script-time dependency directives by default" {
                val tempDir = createTempDirectory("microsmith-script-host-directive-policy")
                try {
                    val script = tempDir.resolve("directive.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        @file:DependsOn("com.acme:dangerous:1.0.0")

                        microsmith { }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )

                    val failure = result.shouldBeTypeOf<ScriptRunFailure>()
                    failure.diagnostics
                        .joinToString("\n") { it.message }
                        .shouldContain("dependency directives are blocked")
                    failure.diagnostics.joinToString("\n") { it.message }.shouldContain("@file:DependsOn")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "blocks grouped file directives that attempt dependency injection" {
                val tempDir = createTempDirectory("microsmith-script-host-grouped-directive-policy")
                try {
                    val script = tempDir.resolve("directive-grouped.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        @file:[
                            Suppress("unused")
                            kotlin.script.experimental.dependencies.DependsOn("com.acme:dangerous:1.0.0")
                        ]

                        microsmith { }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )

                    val failure = result.shouldBeTypeOf<ScriptRunFailure>()
                    failure.diagnostics
                        .joinToString("\n") { it.message }
                        .shouldContain("dependency directives are blocked")
                    failure.diagnostics.joinToString("\n") { it.message }.shouldContain("@file:DependsOn")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "supports optional process isolation mode" {
                val tempDir = createTempDirectory("microsmith-script-host-process-isolation")
                try {
                    val script = tempDir.resolve("schema.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        microsmith {
                            schemas {
                                protobuf {
                                    service("ProcessService") {
                                        "Process" {
                                            request(ProcessRequest)
                                            response(ProcessResponse)
                                        }
                                    }

                                    message("ProcessRequest") {
                                        int32("id") { index(1) }
                                    }

                                    message("ProcessResponse") {
                                        int32("id") { index(1) }
                                    }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                                isolationMode = ScriptIsolationMode.PROCESS,
                            ),
                        )

                    result.shouldBeTypeOf<ScriptRunSuccess>()
                    val generatedFile = output.resolve("proto/ProcessService.proto")
                    Files.exists(generatedFile) shouldBe true
                    generatedFile.readText().shouldContain("rpc Process (ProcessRequest) returns (ProcessResponse);")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "supports forward protobuf message symbols in rpc declarations" {
                val tempDir = createTempDirectory("microsmith-script-host-symbols")
                try {
                    val script = tempDir.resolve("symbols.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        microsmith {
                            schemas {
                                protobuf {
                                    service("Users") {
                                        "GetUsers" {
                                            request(GetUsersRequest)
                                            response(GetUsersResponse)
                                        }
                                    }

                                    message("GetUsersRequest") {
                                        int32("id") { index(1) }
                                    }

                                    message("GetUsersResponse") {
                                        int32("id") { index(1) }
                                    }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )

                    result.shouldBeTypeOf<ScriptRunSuccess>()

                    val generatedFile = output.resolve("proto/Users.proto")
                    generatedFile.exists() shouldBe true

                    generatedFile.readText().shouldContain("rpc GetUsers (GetUsersRequest) returns (GetUsersResponse);")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "rejects enum symbols where rpc message symbols are required" {
                val tempDir = createTempDirectory("microsmith-script-host-symbol-types")
                try {
                    val script = tempDir.resolve("symbol-types.microsmith.kts")
                    val output = tempDir.resolve("generated")
                    val cache = tempDir.resolve("cache")

                    script.writeText(
                        """
                        microsmith {
                            schemas {
                                protobuf {
                                    enum("Role") {
                                        +"USER"
                                    }

                                    service("Users") {
                                        "GetUsers" {
                                            request(Role)
                                            response(Role)
                                        }
                                    }
                                }
                            }
                        }
                        """
                            .trimIndent(),
                    )

                    val host = MicrosmithScriptHost(cacheDirectory = cache)
                    val result =
                        host.run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = output,
                                variables = emptyMap(),
                                flags = emptySet(),
                            ),
                        )

                    val failure = result.shouldBeTypeOf<ScriptRunFailure>()

                    failure.type shouldBe ScriptFailureType.COMPILATION
                    failure.diagnostics.joinToString("\n") { it.message }.shouldContain("MessageRef")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "reports each resolution issue of a classloader run as a coded diagnostic" {
                val tempDir = createTempDirectory("microsmith-script-host-resolution-failure")
                try {
                    val result = runScriptWithInvalidSchema(tempDir, ScriptIsolationMode.CLASSLOADER)

                    val failure = result.shouldBeTypeOf<ScriptRunFailure>()
                    failure.type shouldBe ScriptFailureType.RESOLUTION
                    failure.diagnostics
                        .map { it.code }
                        .shouldContainExactly(
                            "protobuf.unresolved-reference",
                            "protobuf.unresolved-reference",
                        )
                    failure.diagnostics
                        .map { it.message }
                        .let { messages ->
                            messages.single { it.contains("MissingOwner") }
                            messages.single { it.contains("MissingGroup") }
                        }
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "reports each resolution issue of a process isolated run as a coded diagnostic" {
                val tempDir = createTempDirectory("microsmith-script-host-process-resolution-failure")
                try {
                    val result = runScriptWithInvalidSchema(tempDir, ScriptIsolationMode.PROCESS)

                    val failure = result.shouldBeTypeOf<ScriptRunFailure>()
                    failure.type shouldBe ScriptFailureType.RESOLUTION
                    failure.diagnostics
                        .map { it.code }
                        .shouldContainExactly(
                            "protobuf.unresolved-reference",
                            "protobuf.unresolved-reference",
                        )
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }
        },
    )

private fun runScriptWithInvalidSchema(tempDir: Path, isolationMode: ScriptIsolationMode): ScriptRunResult {
    val script = tempDir.resolve("duplicates.microsmith.kts")
    script.writeText(
        """
        microsmith {
            schemas {
                protobuf {
                    "pkg" {
                        message("User") {
                            ref("owner", "MissingOwner") { index(1) }
                            ref("group", "MissingGroup") { index(2) }
                        }
                    }
                }
            }
        }
        """
            .trimIndent(),
    )

    return MicrosmithScriptHost(cacheDirectory = tempDir.resolve("cache"))
        .run(
            ScriptRunRequest(
                script = script,
                outputDir = tempDir.resolve("generated"),
                variables = emptyMap(),
                flags = emptySet(),
                isolationMode = isolationMode,
            ),
        )
}
