package io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspWorkspaceResolver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

private fun DotnetAspWorkspaceResolver.resolveRouteIssues(extension: ServicesExtension): List<DotnetResolutionIssue> =
    resolve(extension).fold(
        ifLeft = { it.toList() },
        ifRight = {
            error("Expected ASP.NET route resolution failure")
        },
    )

class DotnetAspRouteResolutionTests :
    StringSpec({
        "resolution accumulates independent malformed route declarations" {
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
                                "/users/user-{id}" {
                                    get("GetUser") {
                                        responses {
                                            ok("User")
                                        }
                                    }
                                }

                                "admins" {
                                    get("GetAdmin") {
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
                .resolveRouteIssues(
                    builder.requireServicesExtension(),
                ) shouldContainExactly
                listOf(
                    DotnetAspResolutionIssue
                        .InvalidRouteDeclaration(
                            kind =
                            DotnetAspResolutionIssue
                                .RouteDeclarationKind.GROUP,
                            route = "/users/user-{id}",
                            problem =
                            DotnetAspResolutionIssue
                                .RouteProblem
                                .InvalidSegment(
                                    "user-{id}",
                                ),
                        ),
                    DotnetAspResolutionIssue
                        .InvalidRouteDeclaration(
                            kind =
                            DotnetAspResolutionIssue
                                .RouteDeclarationKind.GROUP,
                            route = "admins",
                            problem =
                            DotnetAspResolutionIssue
                                .RouteProblem
                                .MissingLeadingSlash,
                        ),
                )
        }
    })
