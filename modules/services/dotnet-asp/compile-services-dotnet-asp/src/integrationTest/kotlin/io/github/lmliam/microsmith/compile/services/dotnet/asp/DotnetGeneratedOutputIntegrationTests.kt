package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.microsmith
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.packages
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.packages
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.gen.generateTo
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath
import kotlin.io.path.walk
import kotlin.uuid.Uuid

class DotnetGeneratedOutputIntegrationTests :
    StringSpec(
        {
            "generateTo dotnet models writes files byte-identical to the expected output files" {
                val model = microsmith {
                    services {
                        dotnet {
                            target(NET10)
                            solutions {
                                "Platform" {
                                    packages {
                                        "Serilog" {
                                            version("9.0.0")
                                            +"AspNetCore"
                                            "Settings.Configuration" { version("9.0.1") }
                                        }
                                        "FluentValidation" { version("11.9.0") }
                                        "Polly" { version("8.4.0") }
                                    }
                                }
                                "Tools" {}
                            }
                        }

                        "UserService" {
                            dotnet {
                                solution("Platform")
                                project("UserService.Api")
                                packages {
                                    "Serilog" {
                                        +"AspNetCore"
                                        +"Settings.Configuration"
                                    }
                                    +"FluentValidation"
                                    +"Polly"
                                }
                                models {
                                    "User" {
                                        string("id")
                                        string("email")
                                        "address" ref "Address"
                                    }
                                    "Address" {
                                        string("street")
                                        int("number")
                                    }
                                    "Problem" { string("detail") }
                                    "Empty" {}
                                }
                                asp {
                                    ports {
                                        http(7000)
                                        https(7443)
                                    }
                                    rest {
                                        "/users" {
                                            get("/{id}", "GetUser") {
                                                path("GetUserPath") { string("id") }
                                                query("GetUserQuery") {
                                                    bool("includeDetails") {
                                                        optional()
                                                        default(false)
                                                    }
                                                    string("expand") { optional() }
                                                }
                                                headers("GetUserHeaders") {
                                                    header("X-Correlation-Id")
                                                    header("X-Tenant-Id")
                                                }
                                                responses {
                                                    ok("User") {
                                                        headers {
                                                            header("ETag")
                                                            header("Cache-Control")
                                                        }
                                                    }
                                                    notFound("Problem")
                                                }
                                            }
                                            post("CreateUser") {
                                                body("CreateUserBody") {
                                                    string("email")
                                                    "address" ref "Address"
                                                }
                                                responses {
                                                    created("User") { headers { header("Location") } }
                                                    badRequest("Problem")
                                                    conflict("Problem")
                                                }
                                            }
                                            put("/{id}", "ReplaceUser") {
                                                path("ReplaceUserPath") { string("id") }
                                                body("User")
                                                responses { noContent("Empty") }
                                            }
                                            delete("/{id}", "DeleteUser") {
                                                path("DeleteUserPath") { string("id") }
                                                responses { accepted("Problem") }
                                            }
                                        }
                                        "/reports" {
                                            get("/{reportId}", "GetReport") {
                                                path("GetReportPath") { guid("reportId") }
                                                query("GetReportQuery") {
                                                    int("days") { default(30) }
                                                    dateOnly("since")
                                                    dateTimeOffset("requestedAt")
                                                    decimal("threshold") {
                                                        optional()
                                                        default(1.5)
                                                    }
                                                    timeSpan("window") {
                                                        optional()
                                                        default(Duration.ofMinutes(90))
                                                    }
                                                    guid("traceId") {
                                                        default(Uuid.parse("123e4567-e89b-12d3-a456-426614174000"))
                                                    }
                                                    long("limit") { default(100L) }
                                                    char("kind") { default('x') }
                                                }
                                                responses { ok("User") }
                                            }
                                            patch("/{reportId}", "PatchReport") {
                                                path("PatchReportPath") { guid("reportId") }
                                                body("PatchReportBody") {
                                                    string("note")
                                                    dateTime("at")
                                                    timeOnly("time")
                                                    double("score")
                                                    float("ratio")
                                                    byte("flag")
                                                }
                                                responses {
                                                    ok("User")
                                                    unauthorized("Problem")
                                                    forbidden("Problem")
                                                    internalServerError("Problem")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "AuditService" {
                            dotnet {
                                solution("Platform")
                                target(NET11)
                                project("AuditService.Api")
                                packages {
                                    +"Polly"
                                    +"FluentValidation"
                                }
                                models { "AuditEvent" { string("name") } }
                                asp {
                                    rest {
                                        "/audit" {
                                            get("ListEvents") {
                                                query("ListEventsQuery") { int("page") { optional() } }
                                                responses { ok("AuditEvent") }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "ReportService" {
                            dotnet {
                                solution("Tools")
                                project("ReportService.Api")
                                packages {
                                    "Newtonsoft" { "Json" { version("13.0.3") } }
                                    "Humanizer" { "Core" { version("2.14.1") } }
                                }
                                models { "Report" { string("title") } }
                                asp {
                                    ports { http(7100) }
                                    rest { "/reports" { get("ListReports") { responses { ok("Report") } } } }
                                }
                            }
                        }
                    }
                }

                val outputRoot = tempdir("generated-output-").toPath()
                model.generateTo(outputRoot)

                val generated = outputRoot.regularFilesByRelativePath()
                val expected =
                    checkNotNull(javaClass.getResource("/expected-output/dotnet"))
                        .toURI()
                        .toPath()
                        .regularFilesByRelativePath()

                generated.keys shouldBe expected.keys
                generated.filter { (path, bytes) -> !bytes.contentEquals(expected.getValue(path)) }.keys shouldBe
                    emptySet()
            }
        },
    )

private fun Path.regularFilesByRelativePath(): Map<String, ByteArray> =
    walk()
        .filter { it.isRegularFile() }
        .associate { file -> file.relativeTo(this).invariantSeparatorsPathString to file.readBytes() }
