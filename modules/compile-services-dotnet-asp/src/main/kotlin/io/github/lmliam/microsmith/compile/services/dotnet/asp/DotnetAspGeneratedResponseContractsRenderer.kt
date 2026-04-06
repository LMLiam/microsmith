package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint

internal fun renderOperationResultTypes(endpoint: ResolvedDotnetAspEndpoint): String = buildString {
    appendLine("public abstract record ${resultBaseTypeName(endpoint)};")
    appendLine()
    append(
        endpoint.responses.joinToString("\n\n") { response ->
            val parameters = buildList {
                add(resolveResponseModelTypeName(endpoint, response))
                response.headers.forEach { header ->
                    add("string? ${dotnetAspHeaderPropertyName(header.name)} = null")
                }
            }.joinToString(", ")
            "public sealed record ${resultVariantTypeName(endpoint, response)}($parameters) : " +
                "${resultBaseTypeName(endpoint)};"
        },
    )
}
