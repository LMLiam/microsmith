package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.asp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainExactly
import java.nio.file.Path

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetAspWorkspaceResolverTests :
    StringSpec({
        "resolve materializes ASP.NET services with canonical output roots" {
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
                        asp { }
                    }
                }

                "BackOffice" {
                    dotnet {
                        solution("Platform")
                        project("BackOffice.Api")
                    }
                }
            }

            val workspace = DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())

            workspace.servicesByName.mapValues { (_, service) -> service.outputRoot } shouldContainExactly
                mapOf(
                    "UserService" to Path.of("dotnet", "Platform", "UserService.Api"),
                )
        }

        "resolve rejects ASP.NET services that collide on output root" {
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
                        project("Shared.Api")
                        asp { }
                    }
                }

                "AdminService" {
                    dotnet {
                        solution("Platform")
                        project("Shared.Api")
                        asp { }
                    }
                }
            }

            shouldThrow<IllegalArgumentException> {
                DotnetAspWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
        }
    })
