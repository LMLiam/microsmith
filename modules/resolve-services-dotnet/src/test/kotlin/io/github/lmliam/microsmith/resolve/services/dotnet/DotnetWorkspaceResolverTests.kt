package io.github.lmliam.microsmith.resolve.services.dotnet

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetWorkspaceResolverTests :
    StringSpec({
        "resolve materializes inherited target and validated service models" {
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
                            model("User") {
                                string("id")
                                "manager" references "User"
                            }
                        }
                    }
                }
            }

            val workspace = DotnetWorkspaceResolver().resolve(builder.requireServicesExtension())
            val service = requireNotNull(workspace.services["UserService"])

            workspace.target shouldBe io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget.NET8
            workspace.solutions.keys shouldContainExactly listOf("Platform")
            service.solution.name shouldBe "Platform"
            service.project shouldBe "UserService.Api"
            service.models.keys shouldContainExactly listOf("User")
            requireNotNull(service.models["User"]).fields.map { it.name } shouldContainExactly listOf("id", "manager")
        }

        "resolve rejects services that target undeclared solutions" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                    }
                }
            }

            shouldThrow<IllegalStateException> {
                DotnetWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
        }

        "resolve rejects model references to unknown service-local models" {
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
                            model("User") {
                                "manager" ref "MissingUser"
                            }
                        }
                    }
                }
            }

            shouldThrow<IllegalArgumentException> {
                DotnetWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
        }
    })
