package io.github.lmliam.microsmith.resolve.services.dotnet.packages.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class DotnetPackageWorkspaceResolutionDiagnosticMapperTests :
    StringSpec({
        "maps dotnet package issues to diagnostics" {
            val mapper = DotnetPackageWorkspaceResolutionDiagnosticMapper()

            listOf(
                    DotnetPackageWorkspaceResolutionIssue.PackageNotCentrallyOwned(
                        serviceName = "UserService",
                        solutionName = "Platform",
                        packageName = "FluentValidation",
                    ),
                    DotnetPackageWorkspaceResolutionIssue.PackageVersionRequired(
                        serviceName = "UserService",
                        packageName = "Serilog",
                    ),
                    DotnetPackageWorkspaceResolutionIssue.MixedPackageVersionManagement(
                        serviceName = "UserService",
                        solutionName = "Platform",
                        packageName = "Dapper",
                    ),
                )
                .map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "dotnet.packages.package-not-centrally-owned",
                        message =
                            "Dotnet service 'UserService' " +
                                "references package " +
                                "'FluentValidation' but solution " +
                                "'Platform' does not centrally " +
                                "own it.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.packages.package-version-required",
                        message =
                            "Dotnet service 'UserService' " +
                                "references package 'Serilog' " +
                                "without a version and without " +
                                "central package ownership.",
                    ),
                    ResolutionDiagnostic(
                        code = "dotnet.packages.mixed-version-management",
                        message =
                            "Dotnet service 'UserService' " +
                                "declares package 'Dapper' with " +
                                "an explicit version but solution " +
                                "'Platform' uses central package " +
                                "management. Mixed central and " +
                                "direct package version " +
                                "management is not supported " +
                                "within the same solution.",
                    ),
                )
        }
    })
