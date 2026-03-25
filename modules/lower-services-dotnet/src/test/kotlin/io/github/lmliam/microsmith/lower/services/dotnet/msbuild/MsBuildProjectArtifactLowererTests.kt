package io.github.lmliam.microsmith.lower.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MsBuildProjectArtifactLowererTests :
    StringSpec({
        "lower emits deterministic xml with escaped values" {
            val artifact =
                MsBuildProjectArtifact(
                    id = MsBuildProjectArtifactId(
                        solutionName = "Platform",
                        kind = MsBuildProjectKind.DirectoryPackagesProps,
                    ),
                    properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                    items =
                    listOf(
                        MsBuildItem(
                            type = "PackageVersion",
                            include = "Serilog.AspNetCore",
                            metadata = mapOf("Version" to "9.0.0 & preview"),
                        ),
                    ),
                )

            val contribution = MsBuildProjectArtifactLowerer().lower(artifact).single()
            val textContribution = contribution as TextFileArtifactContribution
            val contents = textContribution.contents

            textContribution.artifactId.relativePath shouldBe java.nio.file.Path.of("Directory.Packages.props")
            textContribution.artifactId.outputRoot shouldBe java.nio.file.Path.of("dotnet", "Platform")
            contents.shouldContain("<ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>")
            contents.shouldContain("<PackageVersion Include=\"Serilog.AspNetCore\" Version=\"9.0.0 &amp; preview\" />")
        }
    })
