package io.github.lmliam.microsmith.artifact.services.dotnet.asp.model

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel

public data class DotnetAspModelArtifact(
    public val typeName: String,
    public val locality: DotnetAspModelLocality,
    public val model: DotnetModel,
)
