package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.dsl.core.microsmith
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.asp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class DotnetAspRuntimeE2eTests :
    StringSpec({
        "generated ASP.NET services handle valid and invalid requests end to end".config(enabled = dotnetAvailable()) {
            val outputDir = Files.createTempDirectory("microsmith-dotnet-asp-runtime-")
            try {
                runtimeE2eModel().generateTo(outputDir)

                val projectRoot = outputDir.resolve("dotnet/Platform/UserService.Api")
                val projectFile = projectRoot.resolve("UserService.Api.csproj")
                val logFile = outputDir.resolve("dotnet-runtime.log")
                val port = availablePort()
                val baseUri = URI("http://127.0.0.1:$port")

                runDotnetCommand(
                    projectRoot = projectRoot,
                    logFile = logFile,
                    "build",
                    projectFile.toString(),
                    "--nologo",
                )

                val process = startDotnetService(projectFile, port, logFile)
                try {
                    val client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()

                    awaitServiceReady(client, baseUri, logFile, process)

                    val getUser = client.send(
                        request(baseUri, "/users/user-123?includeDetails=true")
                            .header("X-Correlation-Id", "corr-123")
                            .GET()
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                    getUser.statusCode() shouldBe 200
                    getUser.headers().firstValue("ETag").orElseThrow() shouldBe "sample-etag"
                    getUser.body().shouldContain("\"id\":\"\"")
                    getUser.body().shouldContain("\"email\":\"\"")

                    val notFound = client.send(
                        request(baseUri, "/users/user-123")
                            .header("X-Microsmith-Response-Status", "404")
                            .GET()
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                    notFound.statusCode() shouldBe 404
                    notFound.body().shouldContain("\"detail\":\"\"")

                    val createUser = client.send(
                        request(baseUri, "/users")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("""{"email":"runtime@example.com"}"""))
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                    createUser.statusCode() shouldBe 201
                    createUser.headers().firstValue("Location").orElseThrow() shouldBe "sample-location"

                    val getReport = client.send(
                        request(
                            baseUri,
                            "/reports/550e8400-e29b-41d4-a716-446655440000" +
                                "?days=7" +
                                "&since=2026-04-20" +
                                "&requestedAt=2026-04-20T12:34:56%2B00:00" +
                                "&threshold=9.5" +
                                "&window=01:30:00",
                        ).GET().build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                    getReport.statusCode() shouldBe 200
                    getReport.body().shouldContain("\"title\":\"\"")

                    val invalidGuid = client.send(
                        request(
                            baseUri,
                            "/reports/not-a-guid" +
                                "?days=7" +
                                "&since=2026-04-20" +
                                "&requestedAt=2026-04-20T12:34:56%2B00:00",
                        ).GET().build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                    invalidGuid.statusCode() shouldBe 400
                    invalidGuid.body().shouldContain("path.reportId")

                    val invalidDecimal = client.send(
                        request(
                            baseUri,
                            "/reports/550e8400-e29b-41d4-a716-446655440000" +
                                "?days=7" +
                                "&since=2026-04-20" +
                                "&requestedAt=2026-04-20T12:34:56%2B00:00" +
                                "&threshold=bad",
                        ).GET().build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                    invalidDecimal.statusCode() shouldBe 400
                    invalidDecimal.body().shouldContain("query.threshold")
                } finally {
                    stopProcess(process, logFile)
                }
            } finally {
                runCatching { outputDir.deleteRecursively() }
            }
        }
    })

private fun runtimeE2eModel() =
    microsmith {
        services {
            dotnet {
                target(NET8)
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
                        "Report" {
                            string("id")
                            string("title")
                        }
                    }
                    asp {
                        rest {
                            "/users" {
                                get("/{id}", "GetUser") {
                                    path("GetUserPath") {
                                        string("id")
                                    }
                                    query("GetUserQuery") {
                                        bool("includeDetails") {
                                            optional()
                                            default(false)
                                        }
                                    }
                                    headers("GetUserHeaders") {
                                        header("X-Correlation-Id")
                                    }
                                    responses {
                                        ok("User") {
                                            headers {
                                                header("ETag")
                                            }
                                        }
                                        notFound("Problem")
                                    }
                                }

                                post("CreateUser") {
                                    body("CreateUserBody") {
                                        string("email")
                                    }
                                    responses {
                                        created("User") {
                                            headers {
                                                header("Location")
                                            }
                                        }
                                        badRequest("Problem")
                                    }
                                }
                            }

                            "/reports" {
                                get("/{reportId}", "GetReport") {
                                    path("GetReportPath") {
                                        guid("reportId")
                                    }
                                    query("GetReportQuery") {
                                        int("days")
                                        dateOnly("since")
                                        dateTimeOffset("requestedAt")
                                        decimal("threshold") {
                                            optional()
                                            default(1.5)
                                        }
                                        timeSpan("window") {
                                            optional()
                                        }
                                    }
                                    responses {
                                        ok("Report")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

private fun availablePort(): Int = ServerSocket(0).use { socket -> socket.localPort }

private fun dotnetAvailable(): Boolean = runCatching {
    val process = ProcessBuilder("dotnet", "--version")
        .redirectErrorStream(true)
        .start()
    process.waitFor() == 0
}.getOrDefault(false)

private fun runDotnetCommand(projectRoot: Path, logFile: Path, vararg command: String) {
    val process = ProcessBuilder(listOf("dotnet") + command)
        .directory(projectRoot.toFile())
        .redirectErrorStream(true)
        .redirectOutput(logFile.toFile())
        .start()
    val completed = process.waitFor(2, java.util.concurrent.TimeUnit.MINUTES)
    check(completed) {
        "Timed out running '${command.joinToString(" ")}'.\n${logFileContents(logFile)}"
    }
    check(process.exitValue() == 0) {
        "Command 'dotnet ${command.joinToString(" ")}' failed.\n${logFileContents(logFile)}"
    }
}

private fun startDotnetService(projectFile: Path, port: Int, logFile: Path): Process {
    logFile.parent?.createDirectories()
    logFile.writeText("")
    return ProcessBuilder(
        "dotnet",
        "run",
        "--project",
        projectFile.toString(),
        "--no-build",
        "--no-launch-profile",
        "--nologo",
    ).directory(projectFile.parent.toFile())
        .redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
        .apply {
            environment()["ASPNETCORE_URLS"] = "http://127.0.0.1:$port"
            environment()["DOTNET_CLI_TELEMETRY_OPTOUT"] = "1"
            environment()["DOTNET_NOLOGO"] = "1"
        }.start()
}

private fun awaitServiceReady(client: HttpClient, baseUri: URI, logFile: Path, process: Process) {
    val deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos()
    while (System.nanoTime() < deadline) {
        check(process.isAlive) {
            "Generated ASP.NET service exited before becoming ready.\n${logFileContents(logFile)}"
        }

        val ready = runCatching {
            client.send(
                request(baseUri, "/users/readiness").GET().build(),
                HttpResponse.BodyHandlers.discarding(),
            )
        }.getOrNull()

        if (ready != null && ready.statusCode() in 200..499) {
            return
        }

        Thread.sleep(250)
    }

    error("Generated ASP.NET service did not become ready in time.\n${logFileContents(logFile)}")
}

private fun request(baseUri: URI, pathAndQuery: String): HttpRequest.Builder =
    HttpRequest.newBuilder(baseUri.resolve(pathAndQuery))
        .timeout(Duration.ofSeconds(10))

private fun stopProcess(process: Process, logFile: Path) {
    if (!process.isAlive) {
        return
    }
    process.destroy()
    if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
        process.destroyForcibly()
        check(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            "Unable to stop generated ASP.NET service.\n${logFileContents(logFile)}"
        }
    }
}

private fun logFileContents(logFile: Path): String = when {
    !logFile.exists() -> "<missing log file>"
    else -> logFile.readText()
}
