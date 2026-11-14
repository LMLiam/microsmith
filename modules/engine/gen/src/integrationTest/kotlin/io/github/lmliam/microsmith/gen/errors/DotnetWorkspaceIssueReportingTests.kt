package io.github.lmliam.microsmith.gen.errors

import io.github.lmliam.microsmith.dsl.microsmith
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.gen.generateTo
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspaceResolutionIssue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class DotnetWorkspaceIssueReportingTests :
    StringSpec(
        {
            "a workspace issue is reported once when the packages and ASP.NET modules are present" {
                val model = microsmith {
                    services {
                        dotnet {
                            target(NET10)
                            solutions { "Platform" {} }
                        }

                        "UserService" {
                            dotnet {
                                solution("Platform")
                                asp {}
                            }
                        }
                    }
                }

                val failure =
                    shouldThrow<GenerationResolutionFailedException> {
                        model.generateTo(Files.createTempDirectory("microsmith-workspace-issue-"))
                    }

                failure.issues.toList() shouldBe
                    listOf(DotnetWorkspaceResolutionIssue.ProjectNotConfigured("UserService"))
            }
        },
    )
