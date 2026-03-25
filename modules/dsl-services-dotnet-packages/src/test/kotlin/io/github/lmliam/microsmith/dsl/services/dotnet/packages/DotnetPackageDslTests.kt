package io.github.lmliam.microsmith.dsl.services.dotnet.packages

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.packages
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.packages
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferencesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionsExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetPackageDslTests :
    StringSpec({
        "dotnet solution packages support grouped inheritance and leaf overrides" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
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
            }

            val solution =
                requireNotNull(
                    builder
                        .requireServicesExtension()
                        .get<io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsExtension>(),
                ).requireSolution("Platform")
            val packages = requireNotNull(solution.get<DotnetPackageVersionsExtension>()).packages

            packages shouldBe mapOf(
                "Serilog.AspNetCore" to "9.0.0",
                "Serilog.Settings.Configuration" to "9.0.1",
            )
        }

        "dotnet service packages support string-invoke and unary-plus forms" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
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

                            +"FluentValidation.AspNetCore"
                        }
                    }
                }
            }

            val service =
                builder
                    .requireServicesExtension()
                    .require("UserService")
                    .model
                    .get<io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension>()
            val packages = requireNotNull(requireNotNull(service).get<DotnetPackageReferencesExtension>()).packages

            packages shouldBe setOf(
                "Serilog.AspNetCore",
                "Serilog.Settings.Configuration",
                "FluentValidation.AspNetCore",
            )
        }

        "dotnet package ownership rejects duplicate resolved package identifiers" {
            val builder = MicrosmithBuilder()

            shouldThrow<IllegalArgumentException> {
                builder.services {
                    dotnet {
                        solutions {
                            "Platform" {
                                packages {
                                    "Serilog" {
                                        version("9.0.0")
                                        +"AspNetCore"
                                    }

                                    "Serilog.AspNetCore" {
                                        version("9.0.1")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        "dotnet package ownership rejects invalid names and versions" {
            val builder = MicrosmithBuilder()

            shouldThrow<IllegalArgumentException> {
                builder.services {
                    dotnet {
                        solutions {
                            "Platform" {
                                packages {
                                    "Bad Name" {
                                        version("9.0.0")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            shouldThrow<IllegalArgumentException> {
                builder.services {
                    dotnet {
                        solutions {
                            "Platform" {
                                packages {
                                    "Serilog" {
                                        version(" ")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    })
