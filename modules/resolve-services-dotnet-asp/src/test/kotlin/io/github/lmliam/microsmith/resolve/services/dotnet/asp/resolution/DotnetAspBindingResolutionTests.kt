package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestField
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspBindingResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

private fun DotnetAspWorkspaceResolver.resolveIssues(extension: ServicesExtension): List<DotnetResolutionIssue> =
    resolve(extension).fold(
        ifLeft = { it.toList() },
        ifRight = {
            error("Expected ASP.NET resolution failure")
        },
    )

class DotnetAspBindingResolutionTests :
    StringSpec({
        "resolve rejects path binding mismatches against route placeholders" {
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
                                    get("/{id}", "GetUser") {
                                        path("GetUserPath") {
                                            string("userId")
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

            DotnetAspWorkspaceResolver()
                .resolveIssues(
                    builder.requireServicesExtension(),
                ) shouldContainExactly
                listOf(
                    DotnetAspBindingResolutionIssue
                        .PathBindingFieldMismatch(
                            serviceName = "UserService",
                            operationName = "GetUser",
                            bindingName = "GetUserPath",
                            placeholders = listOf("id"),
                            fields = listOf("userId"),
                        ),
                )
        }

        "resolve accumulates unknown shared model references across body and responses" {
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
                                    post("CreateUser") {
                                        body("MissingModel")

                                        responses {
                                            ok("User")
                                            badRequest("Problem")
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
                    DotnetAspBindingResolutionIssue
                        .UnknownSharedModelReference(
                            serviceName = "UserService",
                            operationName = "CreateUser",
                            source =
                            DotnetAspBindingResolutionIssue
                                .ModelReferenceSource
                                .RequestBody,
                            targetName = "MissingModel",
                        ),
                    DotnetAspBindingResolutionIssue
                        .UnknownSharedModelReference(
                            serviceName = "UserService",
                            operationName = "CreateUser",
                            source =
                            DotnetAspBindingResolutionIssue
                                .ModelReferenceSource
                                .Response(400),
                            targetName = "Problem",
                        ),
                )
        }

        "resolve rejects missing path bindings when route placeholders are present" {
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
                                    get("/{id}", "GetUser") {
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
                    DotnetAspBindingResolutionIssue
                        .MissingPathBinding(
                            serviceName = "UserService",
                            operationName = "GetUser",
                            route = "/users/{id}",
                            placeholders = listOf("id"),
                        ),
                )
        }

        "resolve rejects path bindings when route has no placeholders" {
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
                                        path("ListUsersPath") {
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

            DotnetAspWorkspaceResolver()
                .resolveIssues(
                    builder.requireServicesExtension(),
                ) shouldContainExactly
                listOf(
                    DotnetAspBindingResolutionIssue
                        .PathBindingWithoutPlaceholders(
                            serviceName = "UserService",
                            operationName = "ListUsers",
                            bindingName = "ListUsersPath",
                            route = "/users",
                        ),
                )
        }

        "resolve accumulates optional and defaulted path binding fields" {
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
                                    get(
                                        "/{id}/{page}",
                                        "GetUser",
                                    ) {
                                        path("GetUserPath") {
                                            string("id") {
                                                optional()
                                            }

                                            int("page") {
                                                default(1)
                                            }
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

            DotnetAspWorkspaceResolver()
                .resolveIssues(
                    builder.requireServicesExtension(),
                ) shouldContainExactly
                listOf(
                    DotnetAspBindingResolutionIssue
                        .OptionalPathBindingField(
                            serviceName = "UserService",
                            operationName = "GetUser",
                            bindingName = "GetUserPath",
                            fieldName = "id",
                        ),
                    DotnetAspBindingResolutionIssue
                        .DefaultedPathBindingField(
                            serviceName = "UserService",
                            operationName = "GetUser",
                            bindingName = "GetUserPath",
                            fieldName = "page",
                        ),
                )
        }

        "resolve rejects inline models that reference unknown shared models" {
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
                            "Problem" {
                                string("detail")
                            }
                        }

                        asp {
                            rest {
                                "/users" {
                                    post("CreateUser") {
                                        body("CreateUserBody") {
                                            "manager" ref "MissingUser"
                                        }

                                        responses {
                                            badRequest("Problem")
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
                    DotnetAspBindingResolutionIssue
                        .UnknownSharedModelReference(
                            serviceName = "UserService",
                            operationName = "CreateUser",
                            source =
                            DotnetAspBindingResolutionIssue
                                .ModelReferenceSource
                                .InlineModelField(
                                    modelName =
                                    "CreateUserBody",
                                    fieldName =
                                    "manager",
                                ),
                            targetName = "MissingUser",
                        ),
                )
        }

        "resolution rejects reference typed transport binding fields" {
            val binding =
                DotnetAspRequestBinding(
                    name = "GetUserQuery",
                    fields =
                    listOf(
                        DotnetAspRequestField(
                            name = "user",
                            type =
                            DotnetFieldType
                                .Reference("User"),
                        ),
                    ),
                )

            val result =
                DotnetAspBindingResolver()
                    .resolveRequestBinding(
                        context =
                        DotnetAspOperationContext(
                            serviceName =
                            "UserService",
                            operationName =
                            "GetUser",
                        ),
                        binding = binding,
                    )

            result.fold(
                ifLeft = { issues ->
                    issues.toList() shouldContainExactly
                        listOf(
                            DotnetAspBindingResolutionIssue
                                .RequestBindingReferenceField(
                                    serviceName =
                                    "UserService",
                                    operationName =
                                    "GetUser",
                                    bindingName =
                                    "GetUserQuery",
                                    fieldName = "user",
                                    targetName = "User",
                                ),
                        )
                },
                ifRight = {
                    error(
                        "Expected request binding " +
                            "resolution failure",
                    )
                },
            )
        }
    })
