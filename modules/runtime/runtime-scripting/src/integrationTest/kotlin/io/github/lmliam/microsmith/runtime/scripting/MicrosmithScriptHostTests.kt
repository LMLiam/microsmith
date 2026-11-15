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

                    copyResource("protobuf-user.microsmith.kts", script)

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
                    copyResource("redundant-cast.microsmith.kts", script)
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
                    firstRun.warnings.single().shouldContain("Cast is redundant")
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
                    copyResource("redundant-cast.microsmith.kts", script)
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

                    copyResource("asp-user-service.microsmith.kts", script)

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

                    copyResource("emit-with-context.microsmith.kts", script)

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
                    generatedFile.exists() shouldBe true
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

                    copyResource("asp-user-service-with-problem.microsmith.kts", script)

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

                    copyResource("unknown-dsl.microsmith.kts", script)

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

                    copyResource("depends-on-directive.microsmith.kts", script)

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

                    copyResource("grouped-file-directive.microsmith.kts", script)

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

                    copyResource("process-isolation-service.microsmith.kts", script)

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
                    generatedFile.exists() shouldBe true
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

                    copyResource("forward-symbols.microsmith.kts", script)

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

            "resolves same-named messages in different namespaces by their qualified symbols" {
                val tempDir = createTempDirectory("microsmith-script-host-qualified-symbols")
                try {
                    val result = runScriptResource(tempDir, "qualified-symbols.microsmith.kts")

                    result.shouldBeTypeOf<ScriptRunSuccess>()

                    tempDir
                        .resolve("generated/proto/Users.proto")
                        .readText()
                        .shouldContain("rpc Get (one.User) returns (two.User);")
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "replays the ambiguous plain symbol warning on a compile cache hit" {
                val tempDir = createTempDirectory("microsmith-script-host-symbol-warning-cache")
                try {
                    val resource = "qualified-symbols.microsmith.kts"

                    val firstRun = runScriptResource(tempDir, resource).shouldBeTypeOf<ScriptRunSuccess>()
                    val secondRun = runScriptResource(tempDir, resource).shouldBeTypeOf<ScriptRunSuccess>()

                    firstRun.warnings.size shouldBe 1
                    secondRun.cacheHit shouldBe true
                    secondRun.warnings shouldContainExactly firstRun.warnings
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "fails compilation and reports a warning when a script uses an ambiguous plain symbol" {
                val tempDir = createTempDirectory("microsmith-script-host-ambiguous-symbol")
                try {
                    val result = runScriptResource(tempDir, "ambiguous-plain-symbol.microsmith.kts")

                    val failure = result.shouldBeTypeOf<ScriptRunFailure>()

                    failure.type shouldBe ScriptFailureType.COMPILATION
                    failure.diagnostics.count { it.message.contains("[warning]") } shouldBe 1
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

                    copyResource("enum-symbol-in-rpc.microsmith.kts", script)

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

private const val HOST_SCRIPT_RESOURCES = "/scripting/host"

private fun runScriptResource(tempDir: Path, scriptName: String): ScriptRunResult {
    val script = tempDir.resolve("script.microsmith.kts")
    copyResource(scriptName, script)

    return MicrosmithScriptHost(cacheDirectory = tempDir.resolve("cache"))
        .run(
            ScriptRunRequest(
                script = script,
                outputDir = tempDir.resolve("generated"),
                variables = emptyMap(),
                flags = emptySet(),
            ),
        )
}

private fun copyResource(scriptName: String, target: Path) {
    val resource = "$HOST_SCRIPT_RESOURCES/$scriptName"

    val source =
        checkNotNull(MicrosmithScriptHostTests::class.java.getResource(resource)) { "Missing test resource $resource" }
    target.writeText(source.readText())
}

private fun runScriptWithInvalidSchema(tempDir: Path, isolationMode: ScriptIsolationMode): ScriptRunResult {
    val script = tempDir.resolve("duplicates.microsmith.kts")
    copyResource("unresolved-references.microsmith.kts", script)

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
