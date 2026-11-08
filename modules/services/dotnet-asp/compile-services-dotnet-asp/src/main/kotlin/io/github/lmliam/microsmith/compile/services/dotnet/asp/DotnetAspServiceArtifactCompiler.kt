package io.github.lmliam.microsmith.compile.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.ServicesArtifactCompiler
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.controllerBaseRelativePath
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.microsmithControllerBaseRelativePath
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.DotnetAspProjectRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.msBuildProjectArtifactId
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.renderDotnetAspAppSettingsFile
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.renderDotnetAspLaunchSettingsFile
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.textContribution
import io.github.lmliam.microsmith.compile.services.dotnet.asp.validation.validateEndpointGenerationInputs

@ServiceProvider(ArtifactCompiler::class)
class DotnetAspServiceArtifactCompiler : ServicesArtifactCompiler<DotnetAspServiceArtifact> {
    override val artifactType = DotnetAspServiceArtifact::class

    override fun compile(artifact: DotnetAspServiceArtifact): List<ArtifactContribution<out Artifact>> {
        validateEndpointGenerationInputs(artifact)

        return buildList {
            add(
                MsBuildProjectContribution(
                    artifactId = artifact.msBuildProjectArtifactId(MsBuildProjectKind.Project),
                    projectAttributes = mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web"),
                    properties =
                        mapOf(
                            MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                            MsBuildNames.NULLABLE_PROPERTY to "enable",
                            MsBuildNames.TARGET_FRAMEWORK_PROPERTY to artifact.targetFrameworkMoniker,
                        ),
                )
            )
            add(artifact.textContribution("Program.cs", DotnetAspProjectRenderer.renderProgramFile(artifact)))
            add(artifact.textContribution("appsettings.json", renderDotnetAspAppSettingsFile(artifact)))
            add(
                artifact.textContribution(
                    "Properties/launchSettings.json",
                    renderDotnetAspLaunchSettingsFile(artifact),
                )
            )
            add(
                artifact.textContribution(
                    "Generated/Hosting/MicrosmithHostingExtensions.cs",
                    DotnetAspProjectRenderer.renderHostingExtensionsFile(artifact),
                )
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/ServiceModels.cs",
                    DotnetAspProjectRenderer.renderServiceModelsFile(artifact),
                )
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/RequestModels.cs",
                    DotnetAspProjectRenderer.renderRequestModelsFile(artifact),
                )
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/ResponseModels.cs",
                    DotnetAspProjectRenderer.renderResponseModelsFile(artifact),
                )
            )
            add(
                artifact.textContribution(
                    microsmithControllerBaseRelativePath(),
                    DotnetAspProjectRenderer.renderMicrosmithControllerBaseFile(artifact),
                )
            )
            add(
                artifact.textContribution(
                    controllerBaseRelativePath(artifact),
                    DotnetAspProjectRenderer.renderControllerBaseFile(artifact),
                )
            )
        }
    }
}
