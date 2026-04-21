package io.github.lmliam.microsmith.cli.init

internal object DotnetBootstrapScriptTemplateRenderer {
    fun render(profile: OnboardingProfile): String = buildString {
        appendLine("// Bootstrapped Microsmith ASP.NET service generation for this ${profile.bootstrapTargetDescription}.")
        appendLine("// Canonical first run:")
        appendLine("// microsmith run build.microsmith.kts")
        profile.recommendedOutputDirectory?.let { outputDirectory ->
            appendLine("// Common repository-native output path:")
            appendLine("// microsmith run build.microsmith.kts --out $outputDirectory")
        }
        appendLine(
            """
            microsmith {
                services {
                    dotnet {
                        target(NET8)
                        solutions {
                            "Platform" { }
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
            """.trimIndent(),
        )
    }
}
