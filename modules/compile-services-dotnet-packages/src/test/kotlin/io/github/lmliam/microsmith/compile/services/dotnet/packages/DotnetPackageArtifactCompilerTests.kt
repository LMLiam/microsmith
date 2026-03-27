package io.github.lmliam.microsmith.compile.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReference
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReferencesArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersion
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
                    packages = listOf(
                        DotnetPackageVersion(name = "Serilog.AspNetCore", version = "9.0.0"),
                        DotnetPackageVersion(name = "Serilog.Settings.Configuration", version = "9.0.1"),
                    ),
                )

            val contribution =
                DotnetPackageVersionsArtifactCompiler().compile(artifact).single() as MsBuildProjectContribution

            contribution.artifactId.solutionName shouldBe "Platform"
            contribution.artifactId.projectName shouldBe null
            contribution.artifactId.kind shouldBe MsBuildProjectKind.DirectoryPackagesProps
            contribution.properties shouldContainExactly mapOf("ManagePackageVersionsCentrally" to "true")
            contribution.items.map { it.include to it.attributes } shouldContainExactly listOf(
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
                    packages = listOf(
                        DotnetPackageReference(name = "Serilog.Settings.Configuration", version = null),
                        DotnetPackageReference(name = "Serilog.AspNetCore", version = null),
                    ),
                )

            val contribution =
                DotnetPackageReferencesArtifactCompiler().compile(artifact).single() as MsBuildProjectContribution

            contribution.artifactId.solutionName shouldBe "Platform"
            contribution.artifactId.projectName shouldBe "UserService.Api"
            contribution.artifactId.kind shouldBe MsBuildProjectKind.DirectoryBuildProps
            contribution.properties shouldBe emptyMap()
            contribution.items.map { it.include to it.attributes } shouldContainExactly listOf(
                "Serilog.AspNetCore" to emptyMap(),
                "Serilog.Settings.Configuration" to emptyMap(),
            )
        }

        "compile emits direct per-project package versions into Directory.Build.props" {
            val artifact =
                DotnetPackageReferencesArtifact(
                    id = DotnetPackageReferencesArtifactId(serviceName = "UserService"),
                    solutionName = "Platform",
                    projectName = "UserService.Api",
                    packages = listOf(
                        DotnetPackageReference(name = "Serilog.Settings.Configuration", version = "9.0.1"),
                        DotnetPackageReference(name = "Serilog.AspNetCore", version = "9.0.0"),
                    ),
                )

            val contribution =
                DotnetPackageReferencesArtifactCompiler().compile(artifact).single() as MsBuildProjectContribution

            contribution.artifactId.solutionName shouldBe "Platform"
            contribution.artifactId.projectName shouldBe "UserService.Api"
            contribution.artifactId.kind shouldBe MsBuildProjectKind.DirectoryBuildProps
            contribution.properties shouldBe emptyMap()
            contribution.items.map { it.include to it.attributes } shouldContainExactly listOf(
                "Serilog.AspNetCore" to mapOf("Version" to "9.0.0"),
                "Serilog.Settings.Configuration" to mapOf("Version" to "9.0.1"),
            )
        }
    })
