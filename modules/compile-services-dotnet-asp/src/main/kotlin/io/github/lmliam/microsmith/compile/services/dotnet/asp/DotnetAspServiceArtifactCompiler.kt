package io.github.lmliam.microsmith.compile.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler
import java.nio.file.Path

@ServiceProvider(ArtifactCompiler::class)
class DotnetAspServiceArtifactCompiler : ServicesArtifactCompiler<DotnetAspServiceArtifact> {
    override val artifactType = DotnetAspServiceArtifact::class

    override fun compile(artifact: DotnetAspServiceArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(
            MsBuildProjectContribution(
                artifactId = MsBuildProjectArtifactId(
                    solutionName = artifact.id.solutionName,
                    projectName = artifact.id.projectName,
                    kind = MsBuildProjectKind.Project,
                ),
                projectAttributes = mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web"),
                properties = mapOf(
                    MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                    MsBuildNames.NULLABLE_PROPERTY to "enable",
                    MsBuildNames.TARGET_FRAMEWORK_PROPERTY to artifact.targetFrameworkMoniker,
                ),
            ),
            textContribution(artifact, "Program.cs", renderProgramFile()),
            textContribution(artifact, "appsettings.json", renderAppSettingsFile(artifact)),
            textContribution(artifact, "Properties/launchSettings.json", renderLaunchSettingsFile(artifact)),
        )
    }

    private fun textContribution(
        artifact: DotnetAspServiceArtifact,
        relativePath: String,
        contents: String,
    ): TextFileArtifactContribution {
        return TextFileArtifactContribution(
            artifactId = TextFileArtifactId(
                relativePath = Path.of(relativePath),
                outputRoot = artifact.outputRoot,
            ),
            contents = contents,
        )
    }

    private fun renderProgramFile(): String = """
        var builder = WebApplication.CreateBuilder(args);

        builder.Services.AddControllers();

        var app = builder.Build();

        app.MapControllers();

        app.Run();

        public partial class Program;
    """.trimIndent()

    private fun renderAppSettingsFile(artifact: DotnetAspServiceArtifact): String = """
        {
          "Microsmith": {
            "ServiceName": "${artifact.serviceName}"
          },
          "Logging": {
            "LogLevel": {
              "Default": "Information",
              "Microsoft.AspNetCore": "Warning"
            }
          },
          "AllowedHosts": "*"
        }
    """.trimIndent()

    private fun renderLaunchSettingsFile(artifact: DotnetAspServiceArtifact): String = """
        {
          "${'$'}schema": "http://json.schemastore.org/launchsettings.json",
          "profiles": {
            "${artifact.id.projectName}": {
              "commandName": "Project",
              "dotnetRunMessages": true,
              "launchBrowser": false,
              "applicationUrl": "http://localhost:${artifact.httpPort};https://localhost:${artifact.httpsPort}",
              "environmentVariables": {
                "ASPNETCORE_ENVIRONMENT": "Development"
              }
            }
          }
        }
    """.trimIndent()
}
