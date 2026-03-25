package io.github.lmliam.microsmith.gen.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackageWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageService
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageSolution
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.nio.file.Path

class DotnetPackageArtifactContributorTests :
    StringSpec({
        "contribute creates deterministic solution and service msbuild contributions" {
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
                                    packages =
                                    mapOf(
                                        "Serilog.AspNetCore" to "9.0.0",
                                        "FluentValidation.AspNetCore" to "12.0.0",
                                    ),
                                ),
                        ),
                    ),
                )
                    .map { it as MsBuildProjectContribution }

            contributions shouldContainExactly listOf(
                MsBuildProjectContribution(
                    artifactId = MsBuildProjectArtifactId(
                        relativePath = Path.of("Directory.Packages.props"),
                        outputRoot = Path.of("dotnet", "Platform"),
                    ),
                    properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                    items =
                    listOf(
                        MsBuildItem(
                            type = "PackageVersion",
                            include = "FluentValidation.AspNetCore",
                            metadata = mapOf("Version" to "12.0.0"),
                        ),
                        MsBuildItem(
                            type = "PackageVersion",
                            include = "Serilog.AspNetCore",
                            metadata = mapOf("Version" to "9.0.0"),
                        ),
                    ),
                ),
                MsBuildProjectContribution(
                    artifactId = MsBuildProjectArtifactId(
                        relativePath = Path.of("PackageReferences.props"),
                        outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
                    ),
                    items =
                    listOf(
                        MsBuildItem(
                            type = "PackageReference",
                            include = "FluentValidation.AspNetCore",
                        ),
                        MsBuildItem(
                            type = "PackageReference",
                            include = "Serilog.AspNetCore",
                        ),
                    ),
                ),
            )
        }
    })
