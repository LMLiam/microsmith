package io.github.lmliam.microsmith.compile.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.ServicesArtifactCompiler
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpAttributes
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.controllerBaseRelativePath
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.microsmithControllerBaseRelativePath
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.contract.DotnetAspContractFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.DotnetAspControllerFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.hosting.DotnetAspInfrastructureFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.msBuildProjectArtifactId
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.renderDotnetAspAppSettingsFile
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.renderDotnetAspLaunchSettingsFile
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project.textContribution
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpRenderer

@ServiceProvider(ArtifactCompiler::class)
class DotnetAspServiceArtifactCompiler
internal constructor(
    private val infrastructureRenderer: DotnetAspInfrastructureFileRenderer,
    private val contractRenderer: DotnetAspContractFileRenderer,
    private val controllerRenderer: DotnetAspControllerFileRenderer,
) : ServicesArtifactCompiler<DotnetAspServiceArtifact> {
    private constructor(
        csharpRenderer: CSharpRenderer,
    ) : this(
        DotnetAspInfrastructureFileRenderer(csharpRenderer),
        DotnetAspContractFileRenderer(csharpRenderer),
        DotnetAspControllerFileRenderer(csharpRenderer),
    )

    public constructor() : this(CSharpRenderer())

    override val artifactType = DotnetAspServiceArtifact::class

    override fun compile(artifact: DotnetAspServiceArtifact): List<ArtifactContribution<out Artifact>> {
        return buildList {
            add(
                MsBuildProjectContribution(
                    artifactId = artifact.msBuildProjectArtifactId(),
                    projectAttributes = mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web"),
                    properties =
                        mapOf(
                            MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                            MsBuildNames.NULLABLE_PROPERTY to "enable",
                            MsBuildNames.TARGET_FRAMEWORK_PROPERTY to artifact.targetFrameworkMoniker,
                        ),
                ),
            )
            add(artifact.textContribution("Program.cs", infrastructureRenderer.renderProgramFile(artifact)))
            add(artifact.textContribution("appsettings.json", renderDotnetAspAppSettingsFile(artifact)))
            add(
                artifact.textContribution(
                    "Properties/launchSettings.json",
                    renderDotnetAspLaunchSettingsFile(artifact),
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Hosting/MicrosmithHostingExtensions.cs",
                    infrastructureRenderer.renderHostingExtensionsFile(artifact),
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/ServiceModels.cs",
                    contractRenderer.renderServiceModelsFile(artifact),
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/RequestModels.cs",
                    contractRenderer.renderRequestModelsFile(artifact),
                ),
            )
            add(
                artifact.textContribution(
                    "Generated/Contracts/ResponseModels.cs",
                    contractRenderer.renderResponseModelsFile(artifact),
                ),
            )
            add(
                artifact.textContribution(
                    microsmithControllerBaseRelativePath(),
                    infrastructureRenderer.renderMicrosmithControllerBaseFile(artifact),
                ),
            )
            add(
                artifact.textContribution(
                    controllerBaseRelativePath(artifact),
                    controllerRenderer.renderControllerBaseFile(artifact),
                ),
            )
        }
    }
}
