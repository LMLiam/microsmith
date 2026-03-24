package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget.NET8
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget.NET9
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class DotnetDslTests :
    StringSpec({
        "shared dotnet blocks merge into services extension" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                }
            }

            builder.services {
                dotnet {
                    solutions {
                        "Platform" {}
                    }
                }
            }

            val extension = builder.model.get<ServicesExtension>()!!
            val dotnet = extension.get<DotnetSharedExtension>()!!

            dotnet.target shouldBe NET8
            dotnet.solutions.keys shouldContainExactly listOf("Platform")
        }

        "service dotnet blocks merge into a single service extension" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        target(NET9)
                    }

                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        models {
                            "User" {
                                string("id")
                                int("age")
                                "manager" ref "User"
                            }
                        }
                    }
                }
            }

            val extension = builder.model.get<ServicesExtension>()!!
            val service = extension.require("UserService")
            val dotnet = service.model.get<DotnetServiceExtension>()!!

            dotnet.target shouldBe NET9
            dotnet.solution shouldBe "Platform"
            dotnet.project shouldBe "UserService.Api"
            dotnet.models.keys shouldContainExactly listOf("User")
            val userModel = requireNotNull(dotnet.models["User"])
            userModel.fields.map(DotnetField::name) shouldContainExactly listOf("id", "age", "manager")
            userModel.fields.last().type shouldBe DotnetFieldType.Reference("User")
        }

        "duplicate service dotnet model names are rejected" {
            val builder = MicrosmithBuilder()

            shouldThrow<IllegalArgumentException> {
                builder.services {
                    "UserService" {
                        dotnet {
                            project("UserService.Api")
                            models {
                                "User" {
                                    string("id")
                                }
                            }
                        }

                        dotnet {
                            models {
                                "User" {
                                    string("name")
                                }
                            }
                        }
                    }
                }
            }
        }
    })
