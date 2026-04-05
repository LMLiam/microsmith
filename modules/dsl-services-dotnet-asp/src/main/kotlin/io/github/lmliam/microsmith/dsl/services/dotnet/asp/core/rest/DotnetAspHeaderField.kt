package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class DotnetAspHeaderField(
    val name: String,
    val headerName: String,
) {
    init {
        DotnetField(name, DotnetFieldType.String)
        require(headerName.isNotBlank()) {
            "ASP.NET header name cannot be blank."
        }
    }
}
