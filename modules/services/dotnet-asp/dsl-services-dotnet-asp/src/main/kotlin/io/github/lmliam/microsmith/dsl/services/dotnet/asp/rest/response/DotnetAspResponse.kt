package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.model.DotnetAspModelReference

public data class DotnetAspResponse(
    public val statusCode: Int,
    public val model: DotnetAspModelReference,
    public val headers: List<DotnetAspResponseHeader> = emptyList(),
) {
    init {
        val duplicateHeaders = headers.groupBy { it.name.lowercase() }.filterValues { it.size > 1 }.keys.sorted()
        require(duplicateHeaders.isEmpty()) {
            "ASP.NET response $statusCode declares duplicate headers: " + duplicateHeaders.joinToString(", ") + "."
        }
    }
}
