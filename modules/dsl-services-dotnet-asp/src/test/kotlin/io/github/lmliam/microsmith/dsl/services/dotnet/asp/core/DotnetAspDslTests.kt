package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspPorts
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.asp
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.aspNet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetAspDslTests :
    StringSpec({
        "asp blocks capture grouped routes, bindings, and responses" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        project("UserService.Api")
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

                                    post("CreateUser") {
                                        body("CreateUserRequest") {
                                            string("email")
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

            val services = builder.requireServicesExtension()
            val dotnet = requireNotNull(services.require("UserService").model.get<DotnetServiceExtension>())
            val asp = requireNotNull(dotnet.get<DotnetAspServiceExtension>())
            val rest = requireNotNull(asp.rest)
            val usersGroup = rest.groups.single()
            val getUser = usersGroup.endpoints.single { it.operationName == "GetUser" }
            val createUser = usersGroup.endpoints.single { it.operationName == "CreateUser" }

            usersGroup.path shouldBe "/users"
            getUser.path shouldBe "/{id}"
            requireNotNull(getUser.bindings.path).fields.map { it.name } shouldContainExactly listOf("id")
            createUser.bindings.body shouldBe DotnetAspModelReference.Inline(
                DotnetModel(
                    name = "CreateUserRequest",
                    fields = listOf(
                        DotnetField(
                            name = "email",
                            type = DotnetFieldType.String,
                        ),
                    ),
                ),
            )
        }

        "multiple asp blocks merge declared rest groups" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        asp {
                            rest {
                                "/users" {
                                    get("ListUsers") {
                                        responses {
                                            ok("User")
                                        }
                                    }
                                }
                            }
                        }

                        asp {
                            rest {
                                "/health" {
                                    get("GetHealth") {
                                        responses {
                                            ok("Problem")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val services = builder.requireServicesExtension()
            val dotnet = requireNotNull(services.require("UserService").model.get<DotnetServiceExtension>())
            val asp = requireNotNull(dotnet.get<DotnetAspServiceExtension>())

            requireNotNull(asp.rest).groups.map { it.path } shouldContainExactly listOf("/users", "/health")
        }

        "aspNet alias composes with the existing asp surface" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        aspNet {
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
                    }
                }
            }

            val services = builder.requireServicesExtension()
            val dotnet = requireNotNull(services.require("UserService").model.get<DotnetServiceExtension>())
            val asp = requireNotNull(dotnet.get<DotnetAspServiceExtension>())

            requireNotNull(asp.rest).groups.single().endpoints.single().operationName shouldBe "GetHealth"
        }

        "asp blocks capture explicit launch ports" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        asp {
                            ports {
                                http(7000)
                                https(7443)
                            }
                        }
                    }
                }
            }

            val services = builder.requireServicesExtension()
            val dotnet = requireNotNull(services.require("UserService").model.get<DotnetServiceExtension>())
            val asp = requireNotNull(dotnet.get<DotnetAspServiceExtension>())

            requireNotNull(asp.ports) shouldBe DotnetAspPorts(http = 7000, https = 7443)
        }

        "headers bindings reject colliding derived field names" {
            val builder = MicrosmithBuilder()

            val error =
                shouldThrow<IllegalArgumentException> {
                    builder.services {
                        "UserService" {
                            dotnet {
                                asp {
                                    rest {
                                        "/users" {
                                            get("ListUsers") {
                                                headers("RequestHeaders") {
                                                    header("X-Trace-Id")
                                                    header("X_Trace_Id")
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

            error.message.shouldContain("declares headers with colliding field names: xTraceId")
        }

        "responses support named helpers and custom status codes outside the standard range" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        asp {
                            rest {
                                "/health" {
                                    get("GetHealth") {
                                        responses {
                                            accepted("AcceptedStatus")
                                            status(799, "ProbeStatus")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val services = builder.requireServicesExtension()
            val dotnet = requireNotNull(services.require("UserService").model.get<DotnetServiceExtension>())
            val asp = requireNotNull(dotnet.get<DotnetAspServiceExtension>())
            val responses = requireNotNull(asp.rest).groups.single().endpoints.single().responses

            responses.map { it.statusCode } shouldContainExactly listOf(202, 799)
        }

        "duplicate path bindings are rejected during DSL authoring" {
            val builder = MicrosmithBuilder()

            val error =
                shouldThrow<IllegalArgumentException> {
                    builder.services {
                        "UserService" {
                            dotnet {
                                asp {
                                    rest {
                                        "/users/{id}" {
                                            get("GetUser") {
                                                path("GetUserPath") {
                                                    string("id")
                                                }
                                                path("DuplicatePath") {
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

            error.message.shouldContain("already declares a path binding")
        }

        "duplicate query bindings are rejected during DSL authoring" {
            val builder = MicrosmithBuilder()

            val error =
                shouldThrow<IllegalArgumentException> {
                    builder.services {
                        "UserService" {
                            dotnet {
                                asp {
                                    rest {
                                        "/users" {
                                            get("ListUsers") {
                                                query("ListUsersQuery") {
                                                    string("search")
                                                }
                                                query("DuplicateQuery") {
                                                    string("page")
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

            error.message.shouldContain("already declares a query binding")
        }

        "duplicate body bindings are rejected during DSL authoring" {
            val builder = MicrosmithBuilder()

            val error =
                shouldThrow<IllegalArgumentException> {
                    builder.services {
                        "UserService" {
                            dotnet {
                                asp {
                                    rest {
                                        "/users" {
                                            post("CreateUser") {
                                                body("CreateUserBody")
                                                body("InlineBody") {
                                                    string("email")
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
                }

            error.message.shouldContain("already declares a body binding")
        }

        "duplicate responses blocks are rejected during DSL authoring" {
            val builder = MicrosmithBuilder()

            val error =
                shouldThrow<IllegalArgumentException> {
                    builder.services {
                        "UserService" {
                            dotnet {
                                asp {
                                    rest {
                                        "/health" {
                                            get("GetHealth") {
                                                responses {
                                                    ok("Status")
                                                }
                                                responses {
                                                    accepted("AcceptedStatus")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            error.message.shouldContain("already declares responses")
        }

        "request bindings reject reference-typed transport fields during DSL authoring" {
            val builder = MicrosmithBuilder()

            val error =
                shouldThrow<IllegalArgumentException> {
                    builder.services {
                        "UserService" {
                            dotnet {
                                asp {
                                    rest {
                                        "/users/{id}" {
                                            get("GetUser") {
                                                query("GetUserQuery") {
                                                    "user" ref "User"
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

            error.message.shouldContain("ASP.NET request bindings cannot declare reference field 'user' to 'User'")
        }
    })
