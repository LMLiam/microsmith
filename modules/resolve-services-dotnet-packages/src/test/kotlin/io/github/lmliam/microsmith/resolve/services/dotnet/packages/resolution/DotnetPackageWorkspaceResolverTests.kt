package io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.dotnet
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.packages
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.packages
import io.github.lmliam.microsmith.dsl.services.services
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageReference
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageVersion
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.diagnostics.DotnetPackageWorkspaceResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

private fun MicrosmithBuilder.requireServicesExtension(): ServicesExtension =
    requireNotNull(model.get<ServicesExtension>())

private fun DotnetPackageWorkspaceResolver.resolveSuccessfully(extension: ServicesExtension): DotnetPackageWorkspace =
    resolve(extension)
        .fold(
            ifLeft = { issues -> error("Expected package resolution success, but got: " + issues.joinToString()) },
            ifRight = { it },
        )

private fun DotnetPackageWorkspaceResolver.resolveIssues(extension: ServicesExtension): List<DotnetResolutionIssue> =
    resolve(extension)
        .fold(
            ifLeft = { it.toList() },
            ifRight = { error("Expected package resolution failure") },
        )

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
                                    "Settings.Configuration" { version("9.0.1") }
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

            val workspace = DotnetPackageWorkspaceResolver().resolveSuccessfully(builder.requireServicesExtension())

            workspace.requireSolution("Platform").packages shouldContainExactly
                listOf(
                    ResolvedDotnetPackageVersion(name = "Serilog.AspNetCore", version = "9.0.0"),
                    ResolvedDotnetPackageVersion(name = "Serilog.Settings.Configuration", version = "9.0.1"),
                )
            workspace.requireService("UserService").packages shouldContainExactly
                listOf(
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
                        packages { +"FluentValidation.AspNetCore" }
                    }
                }
            }

            val issues = DotnetPackageWorkspaceResolver().resolveIssues(builder.requireServicesExtension())

            issues shouldContainExactly
                listOf(
                    DotnetPackageWorkspaceResolutionIssue.PackageNotCentrallyOwned(
                        serviceName = "UserService",
                        solutionName = "Platform",
                        packageName = "FluentValidation.AspNetCore",
                    )
                )
        }

        "resolve supports direct per-project package versions when central package management is unused" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                    solutions { "Platform" {} }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        packages {
                            "Serilog" {
                                version("9.0.0")
                                +"AspNetCore"
                                "Settings.Configuration" { version("9.0.1") }
                            }
                        }
                    }
                }
            }

            val workspace = DotnetPackageWorkspaceResolver().resolveSuccessfully(builder.requireServicesExtension())

            workspace.requireService("UserService").packages shouldContainExactly
                listOf(
                    ResolvedDotnetPackageReference(name = "Serilog.AspNetCore", version = "9.0.0"),
                    ResolvedDotnetPackageReference(name = "Serilog.Settings.Configuration", version = "9.0.1"),
                )
        }

        "resolve rejects versionless service package references without central ownership" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)

                    solutions { "Platform" {} }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        packages { +"Serilog.AspNetCore" }
                    }
                }
            }

            val issues = DotnetPackageWorkspaceResolver().resolveIssues(builder.requireServicesExtension())

            issues shouldContainExactly
                listOf(
                    DotnetPackageWorkspaceResolutionIssue.PackageVersionRequired(
                        serviceName = "UserService",
                        packageName = "Serilog.AspNetCore",
                    )
                )
        }

        "resolve rejects direct service package versions when the solution uses central package management" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)
                    solutions { "Platform" { packages { "Serilog.AspNetCore" { version("9.0.0") } } } }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")
                        packages { "FluentValidation.AspNetCore" { version("12.0.0") } }
                    }
                }
            }

            val issues = DotnetPackageWorkspaceResolver().resolveIssues(builder.requireServicesExtension())

            issues shouldContainExactly
                listOf(
                    DotnetPackageWorkspaceResolutionIssue.MixedPackageVersionManagement(
                        serviceName = "UserService",
                        solutionName = "Platform",
                        packageName = "FluentValidation.AspNetCore",
                    )
                )
        }

        "resolve accumulates multiple package resolution issues" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)

                    solutions { "Platform" { packages { "Serilog.AspNetCore" { version("9.0.0") } } } }
                }

                "UserService" {
                    dotnet {
                        solution("Platform")
                        project("UserService.Api")

                        packages {
                            +"FluentValidation.AspNetCore"

                            "Dapper" { version("2.1.66") }
                        }
                    }
                }
            }

            val issues = DotnetPackageWorkspaceResolver().resolveIssues(builder.requireServicesExtension())

            issues shouldContainExactly
                listOf(
                    DotnetPackageWorkspaceResolutionIssue.PackageNotCentrallyOwned(
                        serviceName = "UserService",
                        solutionName = "Platform",
                        packageName = "FluentValidation.AspNetCore",
                    ),
                    DotnetPackageWorkspaceResolutionIssue.MixedPackageVersionManagement(
                        serviceName = "UserService",
                        solutionName = "Platform",
                        packageName = "Dapper",
                    ),
                )
        }

        "resolve keeps package ownership for multiple solutions" {
            val builder = MicrosmithBuilder()

            builder.services {
                dotnet {
                    target(NET8)

                    solutions {
                        "Platform" { packages { "Serilog.AspNetCore" { version("9.0.0") } } }

                        "Payments" { packages { "FluentValidation" { version("12.0.0") } } }
                    }
                }
            }

            val workspace = DotnetPackageWorkspaceResolver().resolveSuccessfully(builder.requireServicesExtension())

            workspace.solutionsByName.keys.sorted() shouldContainExactly
                listOf(
                    "Payments",
                    "Platform",
                )
        }
    })
