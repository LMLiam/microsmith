package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.nio.file.Path

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

private fun DotnetAspWorkspaceResolver.resolveIssues(extension: ServicesExtension): List<DotnetResolutionIssue> =
    resolve(extension).fold(
        ifLeft = { it.toList() },
        ifRight = {
            error("Expected ASP.NET resolution failure")
        },
    )

class DotnetAspWorkspaceCollisionResolutionTests :
    StringSpec({
        "resolve rejects duplicate operation names across grouped routes" {
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
                            "User" {
                                string("id")
                            }
                        }

                        asp {
                            rest {
                                "/users" {
                                    get("GetUser") {
                                        responses {
                                            ok("User")
                                        }
                                    }
                                }

                                "/admins" {
                                    get("GetUser") {
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

            DotnetAspWorkspaceResolver()
                .resolveIssues(
                    builder.requireServicesExtension(),
                ) shouldContainExactly
                listOf(
                    DotnetAspResolutionIssue
                        .DuplicateOperationName(
                            serviceName = "UserService",
                            operationName = "GetUser",
                        ),
                )
        }

        "resolve rejects duplicate method and route mappings across endpoints" {
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
                            "User" {
                                string("id")
                            }
                        }

                        asp {
                            rest {
                                "/users" {
                                    get("ListUsers") {
                                        responses {
                                            ok("User")
                                        }
                                    }
                                }

                                "/users" {
                                    get("GetUsersDuplicate") {
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

            DotnetAspWorkspaceResolver()
                .resolveIssues(
                    builder.requireServicesExtension(),
                ) shouldContainExactly
                listOf(
                    DotnetAspResolutionIssue
                        .DuplicateRestEndpoint(
                            serviceName = "UserService",
                            method = DotnetAspHttpMethod.GET,
                            route = "/users",
                        ),
                )
        }

        "resolve rejects colliding output roots across asp services" {
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
                        project("Shared.Api")
                        asp {}
                    }
                }

                "AdminService" {
                    dotnet {
                        solution("Platform")
                        project("Shared.Api")
                        asp {}
                    }
                }
            }

            DotnetAspWorkspaceResolver()
                .resolveIssues(
                    builder.requireServicesExtension(),
                ) shouldContainExactly
                listOf(
                    DotnetAspResolutionIssue
                        .OutputRootCollision(
                            outputRoot =
                            Path.of(
                                "dotnet",
                                "Platform",
                                "Shared.Api",
                            ),
                            serviceNames =
                            listOf(
                                "AdminService",
                                "UserService",
                            ),
                        ),
                )
        }
    })
