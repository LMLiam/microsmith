package io.github.lmliam.microsmith.gen.services.dotnet

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.NET8
import io.github.lmliam.microsmith.dsl.services.dotnet.core.NET9
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetWorkspaceResolverTests :
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

            val workspace = DotnetWorkspaceResolver().resolve(builder.requireServicesExtension())
            val userService = requireNotNull(workspace.services["UserService"])

            workspace.target shouldBe NET8
            workspace.solutions.keys shouldContainExactly listOf("Platform")
            workspace.services.keys shouldContainExactly listOf("UserService")
            userService.target shouldBe NET8
            userService.project shouldBe "UserService.Api"
            userService.solution.name shouldBe "Platform"
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

            val extension = builder.requireServicesExtension()

            shouldThrow<IllegalArgumentException> {
                DotnetWorkspaceResolver().resolve(extension)
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

            val extension = builder.requireServicesExtension()

            shouldThrow<IllegalStateException> {
                DotnetWorkspaceResolver().resolve(extension)
            }
        }
    })
