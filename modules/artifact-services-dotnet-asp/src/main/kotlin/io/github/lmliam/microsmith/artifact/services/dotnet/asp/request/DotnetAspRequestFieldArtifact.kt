package io.github.lmliam.microsmith.artifact.services.dotnet.asp.request
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType

data class DotnetAspRequestFieldArtifact(
    val name: String,
    val type: DotnetFieldType,
    val optional: Boolean,
    val defaultValue: Any?,
)
