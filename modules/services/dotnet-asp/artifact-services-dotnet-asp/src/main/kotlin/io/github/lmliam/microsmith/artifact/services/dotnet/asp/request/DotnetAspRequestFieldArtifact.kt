package io.github.lmliam.microsmith.artifact.services.dotnet.asp.request

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType

public data class DotnetAspRequestFieldArtifact(
    public val name: String,
    public val type: DotnetFieldType,
    public val optional: Boolean,
    public val defaultValue: Any?,
)
