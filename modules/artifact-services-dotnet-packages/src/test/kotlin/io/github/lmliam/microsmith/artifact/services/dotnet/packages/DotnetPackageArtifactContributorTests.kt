package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackageWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageReference
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageService
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageSolution
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class DotnetPackageArtifactContributorTests :
    StringSpec({
        "contribute creates deterministic solution and service package artifacts" {
            val contributions =
                DotnetPackageArtifactContributor().contribute(
                    DotnetPackageWorkspace(
                        solutions =
                        mapOf(
                            "Platform" to
                                ResolvedDotnetPackageSolution(
                                    name = "Platform",
                                    packages =
                                    mapOf(
                                        "Serilog.AspNetCore" to "9.0.0",
                                        "FluentValidation.AspNetCore" to "12.0.0",
                                    ),
                                ),
                        ),
                        services =
                        mapOf(
                            "UserService" to
                                ResolvedDotnetPackageService(
                                    name = "UserService",
                                    solution = "Platform",
                                    project = "UserService.Api",
                                    packages = listOf(
                                        ResolvedDotnetPackageReference(
                                            name = "FluentValidation.AspNetCore",
                                            version = "12.0.0",
                                        ),
                                        ResolvedDotnetPackageReference(
                                            name = "Serilog.AspNetCore",
                                            version = null,
                                        ),
                                    ),
                                ),
                        ),
                    ),
                )

            contributions shouldContainExactly listOf(
                DotnetPackageVersionsContribution(
                    artifactId = DotnetPackageVersionsArtifactId("Platform"),
                    packages = mapOf(
                        "Serilog.AspNetCore" to "9.0.0",
                        "FluentValidation.AspNetCore" to "12.0.0",
                    ),
                ),
                DotnetPackageReferencesContribution(
                    artifactId = DotnetPackageReferencesArtifactId("UserService"),
                    solutionName = "Platform",
                    projectName = "UserService.Api",
                    packages = listOf(
                        DotnetPackageReference(name = "FluentValidation.AspNetCore", version = "12.0.0"),
                        DotnetPackageReference(name = "Serilog.AspNetCore", version = null),
                    ),
                ),
            )
        }
    })
