package io.github.lmliam.microsmith.resolve.services.dotnet.resolution
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspaceResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

private fun DotnetWorkspaceResolver.resolveSuccessfully(extension: ServicesExtension): DotnetWorkspace =
    resolve(extension).fold(
        ifLeft = { issues ->
            error(
                "Expected .NET resolution success, but got: " +
                    issues.joinToString(),
            )
        },
        ifRight = { it },
    )

private fun DotnetWorkspaceResolver.resolveIssues(extension: ServicesExtension): List<DotnetWorkspaceResolutionIssue> =
    resolve(extension).fold(
        ifLeft = { it.toList() },
        ifRight = {
            error("Expected .NET resolution failure")
        },
    )

class DotnetWorkspaceResolverTests :
    StringSpec({
        "resolve materializes inherited target and validated service models" {
            val builder = MicrosmithBuilder()

            builder.services {
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
                            model("User") {
                                string("id")
                                "manager" references "User"
                            }
                        }
                    }
                }
            }

            val workspace = DotnetWorkspaceResolver().resolveSuccessfully(builder.requireServicesExtension())
            val service = requireNotNull(workspace.services["UserService"])

            workspace.target shouldBe io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget.NET8
            workspace.solutions.keys shouldContainExactly listOf("Platform")
            service.solution.name shouldBe "Platform"
            service.project shouldBe "UserService.Api"
            service.models.keys shouldContainExactly listOf("User")
            requireNotNull(service.models["User"]).fields.map { it.name } shouldContainExactly listOf("id", "manager")
        }

        "resolve rejects services that target undeclared solutions" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                    }
                }
            }

            DotnetWorkspaceResolver()
                .resolveIssues(builder.requireServicesExtension()) shouldContainExactly
                listOf(
                    DotnetWorkspaceResolutionIssue
                        .SolutionNotDeclared(
                            serviceName = "UserService",
                            solutionName = "Platform",
                        ),
                )
        }

        "resolve rejects model references to unknown service-local models" {
            val builder = MicrosmithBuilder()

            builder.services {
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
                            model("User") {
                                "manager" ref "MissingUser"
                            }
                        }
                    }
                }
            }

            DotnetWorkspaceResolver()
                .resolveIssues(builder.requireServicesExtension()) shouldContainExactly
                listOf(
                    DotnetWorkspaceResolutionIssue
                        .UnknownModelReference(
                            serviceName = "UserService",
                            modelName = "User",
                            targetName = "MissingUser",
                        ),
                )
        }
    })
