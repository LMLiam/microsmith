package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class ResolvedDotnetAspHeadersBinding(
    val name: String,
    val headers: List<ResolvedDotnetAspHeaderField>,
) {
    init {
        DotnetField(name, DotnetFieldType.String)
    }
}
