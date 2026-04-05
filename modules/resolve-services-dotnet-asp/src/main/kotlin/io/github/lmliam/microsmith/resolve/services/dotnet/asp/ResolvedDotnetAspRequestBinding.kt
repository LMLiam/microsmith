package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

data class ResolvedDotnetAspRequestBinding(
    val name: String,
    val fields: List<ResolvedDotnetAspRequestField>,
) {
    init {
        DotnetModel(name, fields.map { DotnetField(it.name, it.type) })
    }
}
