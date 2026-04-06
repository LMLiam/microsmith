package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.asp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

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
                                                string("includeDetails") {
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

            val workspace = DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
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
            requireNotNull(getUser.bindings.query).fields.single().optional shouldBe true
            requireNotNull(getUser.bindings.query).fields.single().defaultValue shouldBe false
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

            shouldThrow<IllegalArgumentException> {
                DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
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

            val error =
                shouldThrow<IllegalArgumentException> {
                    DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
                }

            error.message.shouldContain("declares duplicate REST endpoints: GET /users")
        }

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

            shouldThrow<IllegalArgumentException> {
                DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
        }

        "resolve rejects unknown shared model references in bindings and responses" {
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

            shouldThrow<IllegalArgumentException> {
                DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
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

            shouldThrow<IllegalArgumentException> {
                DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
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

            val workspace = DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
            val service = requireNotNull(workspace.servicesByName["UserService"])
            val endpoint = service.rest.endpoints.single()

            service.models.keys.toList() shouldContainExactly listOf("Problem")
            requireNotNull(endpoint.bindings.body).locality shouldBe ResolvedDotnetAspModelLocality.INLINE
            requireNotNull(endpoint.bindings.body).model.name shouldBe "CreateUserBody"
            endpoint.responses.first().model.locality shouldBe ResolvedDotnetAspModelLocality.INLINE
            endpoint.responses.first().model.model.name shouldBe "CreateUserResponse"
            endpoint.responses.first().headers.map { it.name } shouldContainExactly listOf("Location")
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

            val error =
                shouldThrow<IllegalArgumentException> {
                    DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
                }

            error.message.shouldContain("declares a path binding but route '/users' has no placeholders")
        }

        "resolve rejects optional or defaulted path binding fields" {
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
                                            string("id") {
                                                optional()
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

            val error =
                shouldThrow<IllegalArgumentException> {
                    DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
                }

            error.message.shouldContain("cannot declare optional/default fields")
        }

        "resolve rejects malformed route placeholder segments" {
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
                            }
                        }
                    }
                }
            }

            val error =
                shouldThrow<IllegalArgumentException> {
                    DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
                }

            error.message.shouldContain("contains invalid route segment 'user-{id}'")
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

            val error =
                shouldThrow<IllegalArgumentException> {
                    DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
                }

            error.message.shouldContain("references unknown shared model 'MissingUser'")
        }
    })
