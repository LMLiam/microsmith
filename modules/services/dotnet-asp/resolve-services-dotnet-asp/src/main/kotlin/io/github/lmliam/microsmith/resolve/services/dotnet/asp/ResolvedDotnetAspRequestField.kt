package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType

data class ResolvedDotnetAspRequestField(
    val name: String,
    val type: DotnetFieldType,
    val optional: Boolean,
    val defaultValue: DotnetAspDefaultValue?,
)
