package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType

public data class ResolvedDotnetAspRequestField(
    public val name: String,
    public val type: DotnetFieldType,
    public val optional: Boolean,
    public val defaultValue: DotnetAspDefaultValue?,
)
