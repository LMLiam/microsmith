package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildAttributeName
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildPropertyName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MsBuildProjectArtifactCompilerTests :
    StringSpec({
        "compile emits deterministic xml with escaped values" {
            val artifact =
                MsBuildProjectArtifact(
                    id = MsBuildProjectArtifactId(
                        solutionName = "Platform",
                        kind = MsBuildProjectKind.DirectoryPackagesProps,
                    ),
                    properties = mapOf(MsBuildPropertyName.ManagePackageVersionsCentrally to "true"),
                    items =
                    listOf(
                        MsBuildItem(
                            itemName = "PackageVersion",
                            include = "Serilog.AspNetCore",
                            attributes = mapOf(MsBuildAttributeName.Version to "9.0.0 & preview"),
                        ),
                    ),
                )

            val contribution = MsBuildProjectArtifactCompiler().compile(artifact).single()
            val textContribution = contribution as TextFileArtifactContribution
            val contents = textContribution.contents

            textContribution.artifactId.relativePath shouldBe java.nio.file.Path.of("Directory.Packages.props")
            textContribution.artifactId.outputRoot shouldBe java.nio.file.Path.of("dotnet", "Platform")
            contents.shouldContain("<ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>")
            contents.shouldContain("<PackageVersion Include=\"Serilog.AspNetCore\" Version=\"9.0.0 &amp; preview\" />")
        }

        "compile writes project package references into auto-imported Directory.Build.props" {
            val artifact =
                MsBuildProjectArtifact(
                    id = MsBuildProjectArtifactId(
                        solutionName = "Platform",
                        projectName = "UserService.Api",
                        kind = MsBuildProjectKind.DirectoryBuildProps,
                    ),
                    properties = emptyMap(),
                    items =
                    listOf(
                        MsBuildItem(
                            itemName = "PackageReference",
                            include = "Serilog.AspNetCore",
                            attributes = emptyMap(),
                        ),
                    ),
                )

            val contribution = MsBuildProjectArtifactCompiler().compile(artifact).single()
            val textContribution = contribution as TextFileArtifactContribution

            textContribution.artifactId.relativePath shouldBe java.nio.file.Path.of("Directory.Build.props")
            textContribution.artifactId.outputRoot shouldBe
                java.nio.file.Path.of("dotnet", "Platform", "UserService.Api")
            textContribution.contents.shouldContain("<PackageReference Include=\"Serilog.AspNetCore\" />")
        }
    })
