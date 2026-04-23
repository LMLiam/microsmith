package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class ProcessIsolationProtocolTests :
    StringSpec({
        "round-trips request payloads" {
            val tempDir = createTempDirectory("microsmith-process-request-protocol")
            try {
                val requestFile = tempDir.resolve("request.properties")
                val request =
                    ProcessIsolationRequest(
                        request =
                        ScriptRunRequest(
                            script = tempDir.resolve("schema.microsmith.kts"),
                            outputDir = tempDir.resolve("generated"),
                            variables = mapOf("schema" to "User", "package" to "pkg"),
                            flags = setOf("emit", "strict"),
                            pluginClasspath = listOf(tempDir.resolve("plugins/custom.jar")),
                        ),
                        scriptPath = tempDir.resolve("schema.microsmith.kts"),
                        outputPath = tempDir.resolve("generated"),
                        cacheDirectory = tempDir.resolve("cache"),
                    )

                ProcessIsolationProtocol.writeRequest(requestFile, request)
                ProcessIsolationProtocol.readRequest(requestFile) shouldBe request
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "round-trips success payloads with long elapsed millis" {
            val tempDir = createTempDirectory("microsmith-process-success-protocol")
            try {
                val resultFile = tempDir.resolve("result.properties")
                val success =
                    ScriptRunSuccess(
                        warnings = listOf("warning"),
                        cacheHit = true,
                        elapsedMillis = Int.MAX_VALUE.toLong() + 1,
                        generatedRoots =
                        listOf(
                            Path.of("/tmp/generated"),
                            Path.of("/tmp/generated/dotnet/Platform/UserService.Api"),
                        ),
                    )

                ProcessIsolationProtocol.writeResult(resultFile, success)
                ProcessIsolationProtocol.readResult(resultFile).shouldBeInstanceOf<ScriptRunSuccess>() shouldBe success
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "round-trips failure diagnostics and failure type" {
            val tempDir = createTempDirectory("microsmith-process-protocol")
            try {
                val resultFile = tempDir.resolve("result.properties")
                val failure =
                    ScriptRunFailure(
                        diagnostics = listOf("worker failure"),
                        type = ScriptFailureType.COMPILATION,
                    )

                ProcessIsolationProtocol.writeResult(resultFile, failure)
                val decoded = ProcessIsolationProtocol.readResult(resultFile).shouldBeInstanceOf<ScriptRunFailure>()

                decoded shouldBe failure
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "defaults to host failure type for legacy failure payloads" {
            val tempDir = createTempDirectory("microsmith-process-protocol-legacy")
            try {
                val resultFile = tempDir.resolve("legacy-result.properties")
                val properties =
                    Properties().apply {
                        setProperty("result.status", "failure")
                        setProperty("result.diagnostics.count", "1")
                        setProperty("result.diagnostics.0", "legacy failure")
                    }
                Files.newOutputStream(resultFile).use { output ->
                    properties.store(output, "legacy test payload")
                }

                val decoded = ProcessIsolationProtocol.readResult(resultFile).shouldBeInstanceOf<ScriptRunFailure>()

                decoded.diagnostics shouldBe listOf("legacy failure")
                decoded.type shouldBe ScriptFailureType.HOST
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
