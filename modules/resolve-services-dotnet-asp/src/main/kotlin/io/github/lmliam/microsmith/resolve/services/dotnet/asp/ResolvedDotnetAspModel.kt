package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

data class ResolvedDotnetAspModel(
    val locality: ResolvedDotnetAspModelLocality,
    val model: DotnetModel,
)
