package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference

private const val MIN_HTTP_STATUS_CODE = 100
private const val MAX_HTTP_STATUS_CODE = 599

data class DotnetAspResponse(
    val statusCode: Int,
    val model: DotnetAspModelReference,
    val headers: List<DotnetAspResponseHeader> = emptyList(),
) {
    init {
        require(statusCode in MIN_HTTP_STATUS_CODE..MAX_HTTP_STATUS_CODE) {
            "ASP.NET response status code must be between " +
                "$MIN_HTTP_STATUS_CODE and $MAX_HTTP_STATUS_CODE: $statusCode."
        }
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
