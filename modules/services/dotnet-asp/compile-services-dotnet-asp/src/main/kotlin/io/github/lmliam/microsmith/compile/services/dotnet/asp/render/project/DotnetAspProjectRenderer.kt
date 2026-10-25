package io.github.lmliam.microsmith.compile.services.dotnet.asp.render.project

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.contract.DotnetAspContractFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.DotnetAspControllerFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.hosting.DotnetAspInfrastructureFileRenderer

internal object DotnetAspProjectRenderer {
    fun renderProgramFile(artifact: DotnetAspServiceArtifact): String =
        DotnetAspInfrastructureFileRenderer.renderProgramFile(artifact)

    fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String =
        DotnetAspInfrastructureFileRenderer.renderHostingExtensionsFile(artifact)

    fun renderMicrosmithControllerBaseFile(artifact: DotnetAspServiceArtifact): String =
        DotnetAspInfrastructureFileRenderer.renderMicrosmithControllerBaseFile(artifact)

    fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String =
        DotnetAspControllerFileRenderer.renderControllerBaseFile(artifact)

    fun renderServiceModelsFile(artifact: DotnetAspServiceArtifact): String =
        DotnetAspContractFileRenderer.renderServiceModelsFile(artifact)

    fun renderRequestModelsFile(artifact: DotnetAspServiceArtifact): String =
        DotnetAspContractFileRenderer.renderRequestModelsFile(artifact)

    fun renderResponseModelsFile(artifact: DotnetAspServiceArtifact): String =
        DotnetAspContractFileRenderer.renderResponseModelsFile(artifact)
}
