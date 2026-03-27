package io.github.lmliam.microsmith.compile.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReferencesArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersionsArtifactId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

class DotnetPackageArtifactCompilerTests :
    StringSpec({
        "compile emits central package versions into Directory.Packages.props" {
            val artifact =
                DotnetPackageVersionsArtifact(
                    id = DotnetPackageVersionsArtifactId(solutionName = "Platform"),
                    packages = linkedMapOf(
                        "Serilog.AspNetCore" to "9.0.0",
                        "Serilog.Settings.Configuration" to "9.0.1",
                    ),
                )

            val contribution =
                DotnetPackageVersionsArtifactCompiler().compile(artifact).single() as MsBuildProjectContribution

            contribution.artifactId.solutionName shouldBe "Platform"
            contribution.artifactId.projectName shouldBe null
            contribution.artifactId.kind shouldBe MsBuildProjectKind.DirectoryPackagesProps
            contribution.properties shouldContainExactly mapOf("ManagePackageVersionsCentrally" to "true")
            contribution.items.map { it.include to it.metadata } shouldContainExactly listOf(
                "Serilog.AspNetCore" to mapOf("Version" to "9.0.0"),
                "Serilog.Settings.Configuration" to mapOf("Version" to "9.0.1"),
            )
        }

        "compile emits per-project package references into Directory.Build.props" {
            val artifact =
                DotnetPackageReferencesArtifact(
                    id = DotnetPackageReferencesArtifactId(serviceName = "UserService"),
                    solutionName = "Platform",
                    projectName = "UserService.Api",
                    packages = listOf("Serilog.Settings.Configuration", "Serilog.AspNetCore"),
                )

            val contribution =
                DotnetPackageReferencesArtifactCompiler().compile(artifact).single() as MsBuildProjectContribution

            contribution.artifactId.solutionName shouldBe "Platform"
            contribution.artifactId.projectName shouldBe "UserService.Api"
            contribution.artifactId.kind shouldBe MsBuildProjectKind.DirectoryBuildProps
            contribution.properties shouldBe emptyMap()
            contribution.items.map { it.include } shouldContainExactly listOf(
                "Serilog.AspNetCore",
                "Serilog.Settings.Configuration",
            )
        }
    })
