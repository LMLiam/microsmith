package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class DotnetAspRequestBindingArtifact(
    val typeName: String,
    val name: String,
    val fields: List<DotnetAspRequestFieldArtifact>,
    val origins: Set<String>,
)
