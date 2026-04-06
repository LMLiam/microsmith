package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
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
                io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel(
                    name = "CreateUserRequest",
                    fields = listOf(
                        io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField(
                            name = "email",
                            type = io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType.String,
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
    })
