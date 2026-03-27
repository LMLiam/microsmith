package io.github.lmliam.microsmith.resolve.services.dotnet.packages

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.core.services
import io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.packages
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.packages
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

class DotnetPackageWorkspaceResolverTests :
    StringSpec({
        "resolve keeps centrally managed service package references versionless" {
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
                        }
                    }
                }
            }

            val workspace = DotnetPackageWorkspaceResolver().resolve(builder.requireServicesExtension())

            workspace.requireSolution("Platform").packages shouldContainExactly listOf(
                ResolvedDotnetPackageVersion(name = "Serilog.AspNetCore", version = "9.0.0"),
                ResolvedDotnetPackageVersion(name = "Serilog.Settings.Configuration", version = "9.0.1"),
            )
            workspace.requireService("UserService").packages shouldContainExactly listOf(
                ResolvedDotnetPackageReference(name = "Serilog.AspNetCore", version = null),
                ResolvedDotnetPackageReference(name = "Serilog.Settings.Configuration", version = null),
            )
        }

        "resolve rejects service packages that are not centrally owned by the selected solution" {
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

        "resolve supports direct per-project package versions when central package management is unused" {
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

            val workspace = DotnetPackageWorkspaceResolver().resolve(builder.requireServicesExtension())

            workspace.requireService("UserService").packages shouldContainExactly listOf(
                ResolvedDotnetPackageReference(name = "Serilog.AspNetCore", version = "9.0.0"),
                ResolvedDotnetPackageReference(name = "Serilog.Settings.Configuration", version = "9.0.1"),
            )
        }

        "resolve rejects versionless service package references without central ownership" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        packages {
                            +"Serilog.AspNetCore"
                        }
                    }
                }
            }

            shouldThrow<IllegalStateException> {
                DotnetPackageWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
        }

        "resolve rejects direct service package versions when the solution uses central package management" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                    solutions {
                        "Platform" {
                            packages {
                                "Serilog.AspNetCore" {
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
                            "FluentValidation.AspNetCore" {
                                version("12.0.0")
                            }
                        }
                    }
                }
            }

            shouldThrow<IllegalStateException> {
                DotnetPackageWorkspaceResolver().resolve(builder.requireServicesExtension())
            }
        }
    })
