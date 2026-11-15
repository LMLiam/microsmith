package io.github.lmliam.microsmith.artifact.services.dotnet.asp.response

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact

public data class DotnetAspResponseArtifact(
    public val statusCode: Int,
    public val model: DotnetAspModelArtifact,
    public val headers: List<DotnetAspResponseHeaderArtifact>,
)
