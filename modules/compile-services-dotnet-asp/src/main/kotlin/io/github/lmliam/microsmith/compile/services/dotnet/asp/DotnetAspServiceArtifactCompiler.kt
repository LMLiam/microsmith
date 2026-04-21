package io.github.lmliam.microsmith.compile.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler

@ServiceProvider(ArtifactCompiler::class)
class DotnetAspServiceArtifactCompiler : ServicesArtifactCompiler<DotnetAspServiceArtifact> {
    override val artifactType = DotnetAspServiceArtifact::class

    override fun compile(artifact: DotnetAspServiceArtifact): List<ArtifactContribution<out Artifact>> {
        validateEndpointGenerationInputs(artifact)
        val serviceOrigin = setOf("services.${artifact.serviceName}")
        val requestModelOrigins = requestModelOriginsFor(artifact, serviceOrigin)
        val responseModelOrigins = responseModelOriginsFor(artifact, serviceOrigin)
        val controllerOrigins = controllerOriginsFor(artifact, serviceOrigin)

        return buildList {
            add(
                MsBuildProjectContribution(
                    artifactId = artifact.msBuildProjectArtifactId(MsBuildProjectKind.Project),
                    projectAttributes = mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web"),
                    properties = mapOf(
                        MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                        MsBuildNames.NULLABLE_PROPERTY to "enable",
                        MsBuildNames.TARGET_FRAMEWORK_PROPERTY to artifact.targetFrameworkMoniker,
                    ),
                    origins = serviceOrigin,
                ),
            )
            add(
                artifact.textContribution(
                    "Program.cs",
                    DotnetAspProjectRenderer.renderProgramFile(artifact),
                    serviceOrigin,
                ),
            )
            add(artifact.textContribution("appsettings.json", renderDotnetAspAppSettingsFile(artifact), serviceOrigin))
            add(
                artifact.textContribution(
                    "Properties/launchSettings.json",
                    renderDotnetAspLaunchSettingsFile(artifact),
                    serviceOrigin,
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Hosting/MicrosmithHostingExtensions.cs",
                    DotnetAspProjectRenderer.renderHostingExtensionsFile(artifact),
                    serviceOrigin,
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/ServiceModels.cs",
                    DotnetAspProjectRenderer.renderServiceModelsFile(artifact),
                    sharedContractModelOriginsFor(artifact, serviceOrigin),
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/RequestModels.cs",
                    DotnetAspProjectRenderer.renderRequestModelsFile(artifact),
                    requestModelOrigins,
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/ResponseModels.cs",
                    DotnetAspProjectRenderer.renderResponseModelsFile(artifact),
                    responseModelOrigins,
                ),
            )
            add(
                artifact.textContribution(
                    microsmithControllerBaseRelativePath(),
                    DotnetAspProjectRenderer.renderMicrosmithControllerBaseFile(artifact),
                    serviceOrigin,
                ),
            )
            add(
                artifact.textContribution(
                    controllerBaseRelativePath(artifact),
                    DotnetAspProjectRenderer.renderControllerBaseFile(artifact),
                    controllerOrigins,
                ),
            )
        }
    }
}
