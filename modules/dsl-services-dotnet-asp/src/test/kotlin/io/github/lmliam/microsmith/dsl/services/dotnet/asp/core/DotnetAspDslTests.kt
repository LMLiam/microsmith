package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.asp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetAspDslTests :
    StringSpec({
        "asp blocks opt a .NET service into ASP.NET scaffolding" {
            val builder = MicrosmithBuilder()

            builder.services {
                "UserService" {
                    dotnet {
                        project("UserService.Api")
                        asp { }
                    }
                }
            }

            val services = builder.requireServicesExtension()
            val dotnet =
                requireNotNull(
                    services.require("UserService").model.get<DotnetServiceExtension>(),
                )

            requireNotNull(dotnet.get<DotnetAspServiceExtension>()) shouldBeSameInstanceAs DotnetAspServiceExtension
        }
    })
