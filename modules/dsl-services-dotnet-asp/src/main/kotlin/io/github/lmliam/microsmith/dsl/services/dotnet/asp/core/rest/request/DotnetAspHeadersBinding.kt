package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class DotnetAspHeadersBinding(
    val name: String,
    val headers: List<DotnetAspHeaderField>,
) {
    init {
        require(headers.isNotEmpty()) {
            "ASP.NET headers binding '$name' must declare at least one header."
        }
        DotnetField(name, DotnetFieldType.String)
        val duplicateHeaders =
            headers
                .groupBy { it.headerName.lowercase() }
                .filterValues { it.size > 1 }
                .keys
                .sorted()
        require(duplicateHeaders.isEmpty()) {
            "ASP.NET headers binding '$name' declares duplicate headers: " +
                duplicateHeaders.joinToString(", ") + "."
        }
        val duplicateFields =
            headers
                .groupBy(DotnetAspHeaderField::name)
                .filterValues { it.size > 1 }
                .keys
                .sorted()
        require(duplicateFields.isEmpty()) {
            "ASP.NET headers binding '$name' declares headers with colliding field names: " +
                duplicateFields.joinToString(", ") + "."
        }
    }
}
