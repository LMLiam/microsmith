package io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.response.DotnetAspResponseArtifact

data class DotnetAspEndpointArtifact(
    val method: String,
    val route: String,
    val operationName: String,
    val bindings: DotnetAspEndpointBindingsArtifact,
    val responses: List<DotnetAspResponseArtifact>,
    val origins: Set<String>,
)
