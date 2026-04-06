package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal class DotnetAspEndpointTextFileRenderer {
    fun render(artifact: DotnetAspServiceArtifact): List<Pair<String, String>> {
        validateEndpointGenerationInputs(artifact)

        return buildList {
            add("Generated/Hosting/MicrosmithHostingExtensions.cs" to renderHostingExtensionsFile(artifact))
            renderSharedModelsFile(artifact)?.let {
                add("Generated/Contracts/ServiceModels.cs" to it)
            }
            renderRequestModelsFile(artifact)?.let {
                add("Generated/Contracts/RequestModels.cs" to it)
            }
            renderResponseModelsFile(artifact)?.let {
                add("Generated/Contracts/ResponseModels.cs" to it)
            }
            renderMicrosmithControllerBaseFile(artifact)?.let {
                add("Generated/Controllers/MicrosmithControllerBase.cs" to it)
            }
            renderControllerBaseFile(artifact)?.let {
                add("Generated/Controllers/${controllerPrefix(artifact)}ControllerBase.cs" to it)
            }
        }
    }
}
