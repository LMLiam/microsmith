package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import java.nio.file.Path

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

private fun DotnetAspWorkspaceResolver.resolveSuccessfully(extension: ServicesExtension): DotnetAspWorkspace =
    resolve(extension).fold(
        ifLeft = { issues ->
            error(
                "Expected ASP.NET resolution success, but got: " +
                    issues.joinToString(),
            )
        },
        ifRight = { it },
    )

class DotnetAspWorkspaceResolverTests :
    StringSpec({
        "resolve materializes normalized rest endpoints, bindings, and response metadata" {
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
                            "Problem" {
                                string("detail")
                            }
                        }
                        asp {
                            rest {
                                "/users" {
                                    "/{id}" {
                                        get("GetUser") {
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
                                    }

                                    post("CreateUser") {
                                        body("CreateUserBody") {
                                            string("email")
                                            "manager" ref "User"
                                        }
                                        responses {
                                            created("User")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val workspace = DotnetAspWorkspaceResolver().resolveSuccessfully(builder.requireServicesExtension())
            val service = requireNotNull(workspace.servicesByName["UserService"])
            val endpoints = service.rest.endpoints.associateBy(ResolvedDotnetAspEndpoint::operationName)
            val getUser = requireNotNull(endpoints["GetUser"])
            val createUser = requireNotNull(endpoints["CreateUser"])

            workspace.servicesByName shouldContainKey "UserService"
            service.outputRoot shouldBe Path.of("dotnet", "Platform", "UserService.Api")
            service.models.keys.toList() shouldContainExactly listOf("User", "Problem")

            getUser.route shouldBe "/users/{id}"
            getUser.routePlaceholders shouldContainExactly listOf("id")
            requireNotNull(getUser.bindings.path).fields.single().type shouldBe DotnetFieldType.String
            requireNotNull(getUser.bindings.query).fields.single().type shouldBe DotnetFieldType.Bool
            requireNotNull(getUser.bindings.query).fields.single().optional shouldBe true
            requireNotNull(getUser.bindings.query).fields.single().defaultValue shouldBe
                DotnetAspDefaultValue.BooleanValue(false)
            requireNotNull(getUser.bindings.headers).headers.single().headerName shouldBe "X-Correlation-Id"
            getUser.responses.map { it.statusCode } shouldContainExactly listOf(200, 404)
            getUser.responses.first().headers.map { it.name } shouldContainExactly listOf("ETag")
            getUser.responses.first().model.locality shouldBe ResolvedDotnetAspModelLocality.SHARED

            createUser.route shouldBe "/users"
            requireNotNull(createUser.bindings.body).locality shouldBe ResolvedDotnetAspModelLocality.INLINE
            requireNotNull(createUser.bindings.body)
                .model
                .fields
                .map { it.name } shouldContainExactly listOf("email", "manager")
            createUser.responses.single().model.locality shouldBe ResolvedDotnetAspModelLocality.SHARED
        }

        "resolve preserves explicit asp net ports" {
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
                        asp {
                            ports {
                                http(7000)
                                https(7443)
                            }
                            rest {
                                "/health" {
                                    get("GetHealth") {
                                        responses {
                                            ok("Status")
                                        }
                                    }
                                }
                            }
                        }
                        models {
                            "Status" {
                                string("value")
                            }
                        }
                    }
                }
            }

            val workspace = DotnetAspWorkspaceResolver().resolveSuccessfully(builder.requireServicesExtension())

            requireNotNull(workspace.servicesByName["UserService"]).ports shouldBe
                ResolvedDotnetAspPorts(http = 7000, https = 7443)
        }

        "resolve keeps inline request and response models local to their endpoint" {
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
                                            string("email")
                                        }
                                        responses {
                                            created("CreateUserResponse") {
                                                model {
                                                    string("id")
                                                }
                                                headers {
                                                    header("Location")
                                                }
                                            }
                                            badRequest("Problem")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val workspace = DotnetAspWorkspaceResolver().resolveSuccessfully(builder.requireServicesExtension())
            val service = requireNotNull(workspace.servicesByName["UserService"])
            val endpoint = service.rest.endpoints.single()

            service.models.keys.toList() shouldContainExactly listOf("Problem")
            requireNotNull(endpoint.bindings.body).locality shouldBe ResolvedDotnetAspModelLocality.INLINE
            requireNotNull(endpoint.bindings.body).model.name shouldBe "CreateUserBody"
            endpoint.responses.first().model.locality shouldBe ResolvedDotnetAspModelLocality.INLINE
            endpoint.responses.first().model.model.name shouldBe "CreateUserResponse"
            endpoint.responses.first().headers.map { it.name } shouldContainExactly listOf("Location")
        }
    })
