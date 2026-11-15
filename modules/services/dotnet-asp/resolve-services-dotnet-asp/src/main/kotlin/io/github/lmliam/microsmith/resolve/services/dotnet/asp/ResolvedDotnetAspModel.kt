package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel

public data class ResolvedDotnetAspModel(
    public val locality: ResolvedDotnetAspModelLocality,
    public val model: DotnetModel,
)
