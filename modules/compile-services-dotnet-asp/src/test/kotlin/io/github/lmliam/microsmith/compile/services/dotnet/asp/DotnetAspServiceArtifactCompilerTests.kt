package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path

class DotnetAspServiceArtifactCompilerTests :
    StringSpec({
        "compile emits the base ASP.NET scaffold artefacts" {
            val artifact =
                DotnetAspServiceArtifact(
                    id = DotnetAspServiceArtifactId(solutionName = "Platform", projectName = "UserService.Api"),
                    serviceName = "UserService",
                    targetFrameworkMoniker = "net8.0",
                    outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
                    httpPort = 5000,
                    httpsPort = 5001,
                )

            val contributions = DotnetAspServiceArtifactCompiler().compile(artifact)
            val msbuild = contributions.filterIsInstance<MsBuildProjectContribution>().single()
            val textFiles = contributions.filterIsInstance<TextFileArtifactContribution>()

            msbuild.artifactId.kind shouldBe MsBuildProjectKind.Project
            msbuild.projectAttributes shouldContainExactly
                mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web")
            msbuild.properties shouldContainExactly mapOf(
                MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                MsBuildNames.NULLABLE_PROPERTY to "enable",
                MsBuildNames.TARGET_FRAMEWORK_PROPERTY to "net8.0",
            )

            textFiles.map { it.artifactId.relativePath.toString() } shouldContainExactlyInAnyOrder listOf(
                "Program.cs",
                "appsettings.json",
                "Properties/launchSettings.json",
            )
            textFiles.forEach {
                it.artifactId.outputRoot shouldBe Path.of("dotnet", "Platform", "UserService.Api")
            }
            textFiles.single { it.artifactId.relativePath.toString() == "Program.cs" }.contents
                .shouldContain("AddControllers")
            textFiles.single { it.artifactId.relativePath.toString() == "Program.cs" }.contents
                .shouldContain("public partial class Program { }")
            textFiles.single { it.artifactId.relativePath.toString() == "appsettings.json" }.contents
                .shouldContain(
                    "\"ServiceName\": \"UserService\"",
                )
            textFiles.single { it.artifactId.relativePath.toString() == "Properties/launchSettings.json" }.contents
                .shouldContain("http://localhost:5000;https://localhost:5001")
        }

        "compile escapes service names before embedding them in appsettings json" {
            val artifact =
                DotnetAspServiceArtifact(
                    id = DotnetAspServiceArtifactId(solutionName = "Platform", projectName = "UserService.Api"),
                    serviceName = "User\"Service\\Api",
                    targetFrameworkMoniker = "net8.0",
                    outputRoot = Path.of("dotnet", "Platform", "UserService.Api"),
                    httpPort = 5000,
                    httpsPort = 5001,
                )

            val appSettings =
                DotnetAspServiceArtifactCompiler()
                    .compile(artifact)
                    .filterIsInstance<TextFileArtifactContribution>()
                    .single { it.artifactId.relativePath.toString() == "appsettings.json" }
                    .contents

            appSettings.shouldContain("\"ServiceName\": \"User\\\"Service\\\\Api\"")
            appSettings.shouldNotContain("\"ServiceName\": \"User\"Service\\Api\"")
        }
    })
