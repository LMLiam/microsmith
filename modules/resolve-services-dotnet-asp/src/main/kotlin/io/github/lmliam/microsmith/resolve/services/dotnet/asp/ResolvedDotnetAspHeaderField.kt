package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class ResolvedDotnetAspHeaderField(
    val name: String,
    val headerName: String,
) {
    init {
        DotnetField(name, DotnetFieldType.String)
    }
}
