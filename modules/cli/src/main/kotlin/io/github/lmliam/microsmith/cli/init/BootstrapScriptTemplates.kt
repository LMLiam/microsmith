package io.github.lmliam.microsmith.cli.init

internal object BootstrapScriptTemplates {
    fun filesFor(repositoryDetection: OnboardingProfileDetection): Map<String, String> = linkedMapOf(
        "settings.microsmith.kts" to renderDefaultSettingsScript(repositoryDetection),
        "build.microsmith.kts" to renderDefaultBuildScript(repositoryDetection.profile),
    )

    private fun renderDefaultSettingsScript(repositoryDetection: OnboardingProfileDetection): String = buildString {
        appendLine("// Microsmith repository settings.")
        appendLine("// ${repositoryDetection.describeForComment()}.")
        appendLine("// Add shared script configuration here as your repository grows.")
    }

    private fun renderDefaultBuildScript(profile: OnboardingProfile): String = buildString {
        if (profile == DotnetOnboardingProfile) {
            append(renderDotnetBuildScript(profile))
            return@buildString
        }
        appendLine("// Bootstrapped Microsmith schema for this ${profile.bootstrapTargetDescription}.")
        appendLine("// Canonical first run:")
        appendLine("// microsmith run build.microsmith.kts")
        appendLine("// Generated .proto files land in ./proto by default.")
        profile.recommendedOutputDirectory?.let { outputDirectory ->
            appendLine("// Common repository-native output path:")
            appendLine("// microsmith run build.microsmith.kts --out $outputDirectory")
        }
        appendLine(renderDefaultBuildScriptBody(profile))
    }

    private fun renderDefaultBuildScriptBody(profile: OnboardingProfile): String = when (profile) {
        DotnetOnboardingProfile -> renderDotnetBuildScriptBody()
        else -> renderSchemaOnlyBuildScriptBody(profile)
    }

    private fun renderSchemaOnlyBuildScriptBody(profile: OnboardingProfile): String {
        return """
            microsmith {
                schemas {
                    protobuf {
                        message("${profile.sampleMessageName}") {
                            int32("id") { index(1) }
                            string("email") { index(2) }
                        }
                    }
                }
            }
        """.trimIndent()
    }

    private fun renderDotnetBuildScriptBody(): String {
        return """
            microsmith {
                schemas {
                    protobuf {
                        message("${DotnetOnboardingProfile.sampleMessageName}") {
                            int32("id") { index(1) }
                            string("email") { index(2) }
                        }
                    }
                }

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
        """.trimIndent()
    }

    private fun renderDotnetBuildScript(profile: OnboardingProfile): String = buildString {
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
