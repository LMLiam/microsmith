package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal class DotnetAspEndpointTextFileRenderer {
    fun render(artifact: DotnetAspServiceArtifact): List<DotnetAspGeneratedTextFile> {
        validateEndpointGenerationInputs(artifact)

        return listOf(
            DotnetAspGeneratedTextFile(
                relativePath = "Generated/Hosting/MicrosmithHostingExtensions.cs",
                contents = renderHostingExtensionsFile(artifact),
            ),
            DotnetAspGeneratedTextFile(
                relativePath = "Generated/Contracts/ServiceModels.cs",
                contents = renderSharedModelsFile(artifact),
            ),
            DotnetAspGeneratedTextFile(
                relativePath = "Generated/Contracts/RequestModels.cs",
                contents = renderRequestModelsFile(artifact),
            ),
            DotnetAspGeneratedTextFile(
                relativePath = "Generated/Contracts/ResponseModels.cs",
                contents = renderResponseModelsFile(artifact),
            ),
            DotnetAspGeneratedTextFile(
                relativePath = microsmithControllerBaseRelativePath(),
                contents = renderMicrosmithControllerBaseFile(artifact),
            ),
            DotnetAspGeneratedTextFile(
                relativePath = controllerBaseRelativePath(artifact),
                contents = renderControllerBaseFile(artifact),
            ),
        )
    }
}
