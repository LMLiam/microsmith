package io.github.lmliam.microsmith.gen.services.dotnet

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget.NET8
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget.NET9
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class DotnetGenerationServiceTests :
    StringSpec({
        "resolve inherits shared target and resolves services" {
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
                                int("age")
                            }
                        }
                    }
                }
            }

            val workspace = DotnetGenerationService().resolve(builder.model.get<ServicesExtension>()!!)

            workspace.target shouldBe NET8
            workspace.solutions.keys shouldContainExactly listOf("Platform")
            workspace.services.keys shouldContainExactly listOf("UserService")
            workspace.services["UserService"]?.target shouldBe NET8
            workspace.services["UserService"]?.project shouldBe "UserService.Api"
            workspace.services["UserService"]?.solution?.name shouldBe "Platform"
        }

        "resolve rejects unknown dotnet model references" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET9)
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
                                "manager" ref "Missing"
                            }
                        }
                    }
                }
            }

            val extension = builder.model.get<ServicesExtension>()!!

            shouldThrow<IllegalArgumentException> {
                DotnetGenerationService().resolve(extension)
            }
        }

        "resolve rejects services without dotnet target when no shared target exists" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    solutions {
                        "Platform" {}
                    }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                    }
                }
            }

            val extension = builder.model.get<ServicesExtension>()!!

            shouldThrow<IllegalStateException> {
                DotnetGenerationService().resolve(extension)
            }
        }
    })
