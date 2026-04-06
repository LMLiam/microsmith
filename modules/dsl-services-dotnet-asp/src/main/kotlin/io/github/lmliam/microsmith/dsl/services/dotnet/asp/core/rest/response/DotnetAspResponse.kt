package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference

data class DotnetAspResponse(
    val statusCode: Int,
    val model: DotnetAspModelReference,
    val headers: List<DotnetAspResponseHeader> = emptyList(),
) {
    init {
        val duplicateHeaders =
            headers
                .groupBy { it.name.lowercase() }
                .filterValues { it.size > 1 }
                .keys
                .sorted()
        require(duplicateHeaders.isEmpty()) {
            "ASP.NET response $statusCode declares duplicate headers: " +
                duplicateHeaders.joinToString(", ") + "."
        }
    }
}
