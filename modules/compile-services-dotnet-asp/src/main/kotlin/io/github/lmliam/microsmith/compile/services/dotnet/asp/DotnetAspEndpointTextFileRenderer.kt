package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal class DotnetAspEndpointTextFileRenderer {
    fun render(artifact: DotnetAspServiceArtifact): List<DotnetAspGeneratedTextFile> {
        validateEndpointGenerationInputs(artifact)

        return buildList {
            add(
                DotnetAspGeneratedTextFile(
                    relativePath = "Generated/Hosting/MicrosmithHostingExtensions.cs",
                    contents = renderHostingExtensionsFile(artifact),
                ),
            )
            renderSharedModelsFile(artifact)?.let {
                add(DotnetAspGeneratedTextFile("Generated/Contracts/ServiceModels.cs", it))
            }
            renderRequestModelsFile(artifact)?.let {
                add(DotnetAspGeneratedTextFile("Generated/Contracts/RequestModels.cs", it))
            }
            renderResponseModelsFile(artifact)?.let {
                add(DotnetAspGeneratedTextFile("Generated/Contracts/ResponseModels.cs", it))
            }
            renderMicrosmithControllerBaseFile(artifact)?.let {
                add(DotnetAspGeneratedTextFile("Generated/Controllers/MicrosmithControllerBase.cs", it))
            }
            renderControllerBaseFile(artifact)?.let {
                add(
                    DotnetAspGeneratedTextFile(
                        relativePath = "Generated/Controllers/${controllerPrefix(artifact)}ControllerBase.cs",
                        contents = it,
                    ),
                )
            }
        }
    }
}
