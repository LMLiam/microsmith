package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

data class DotnetAspModelArtifact(
    val typeName: String,
    val locality: DotnetAspModelLocality,
    val model: DotnetModel,
    val origins: Set<String>,
)
