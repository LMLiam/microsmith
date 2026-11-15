package io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.response.DotnetAspResponseArtifact

public data class DotnetAspEndpointArtifact(
    public val method: String,
    public val route: String,
    public val operationName: String,
    public val bindings: DotnetAspEndpointBindingsArtifact,
    public val responses: List<DotnetAspResponseArtifact>,
)
