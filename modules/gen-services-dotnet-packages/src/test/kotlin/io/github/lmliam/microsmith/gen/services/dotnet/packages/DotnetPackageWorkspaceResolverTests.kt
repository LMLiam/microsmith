package io.github.lmliam.microsmith.gen.services.dotnet.packages

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.packages
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.packages
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetPackageWorkspaceResolverTests :
    StringSpec({
        "resolve normalizes package ownership and package references" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET9)
                    solutions {
                        "Platform" {
                            packages {
                                "Serilog" {
                                    version("9.0.0")
                                    +"AspNetCore"
                                    "Settings.Configuration" {
                                        version("9.0.1")
                                    }
                                }
                            }
                        }
                    }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        packages {
                            "Serilog" {
                                +"AspNetCore"
                                +"Settings.Configuration"
                            }
                        }
                    }
                }
            }

            val workspace = DotnetPackageWorkspaceResolver().resolve(builder.requireServicesExtension())

            workspace.solutions.keys shouldBe setOf("Platform")
            workspace.solutions["Platform"]?.packages shouldBe mapOf(
                "Serilog.AspNetCore" to "9.0.0",
                "Serilog.Settings.Configuration" to "9.0.1",
            )
            workspace.services.keys shouldBe setOf("UserService")
            workspace.services["UserService"]?.packages shouldBe mapOf(
                "Serilog.AspNetCore" to "9.0.0",
                "Serilog.Settings.Configuration" to "9.0.1",
            )
        }

        "resolve rejects undeclared package references" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET9)
                    solutions {
                        "Platform" {
                            packages {
                                "Serilog" {
                                    version("9.0.0")
                                }
                            }
                        }
                    }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        packages {
                            +"FluentValidation.AspNetCore"
                        }
                    }
                }
            }

            shouldThrow<IllegalStateException> {
                DotnetPackageWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
        }
    })
