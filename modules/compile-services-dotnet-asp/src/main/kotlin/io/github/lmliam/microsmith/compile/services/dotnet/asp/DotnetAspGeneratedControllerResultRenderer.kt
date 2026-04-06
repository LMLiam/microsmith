package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal fun renderResultMapper(endpoint: ResolvedDotnetAspEndpoint): String = buildString {
    appendLine(
        "private IActionResult Map${endpoint.operationName}Result(" +
            "${resultBaseTypeName(endpoint)} result)",
    )
    appendLine("{")
    appendLine(dotnetAspIndent("return result switch"))
    appendLine(dotnetAspIndent("{"))
    append(dotnetAspIndent(renderResponseSwitchArms(endpoint), spaces = 8))
    appendLine(",")
    appendLine(dotnetAspIndent(renderUnsupportedResultArm(endpoint), spaces = 8))
    appendLine(dotnetAspIndent("};"))
    append("}")
}

internal fun renderRespondHelper(): String = """
    private IActionResult Respond(
        object body,
        int statusCode,
        params (string Name, string? Value)[] headers
    )
    {
        foreach (var (name, value) in headers)
        {
            if (value is not null)
            {
                Response.Headers[name] = value;
            }
        }

        return new ObjectResult(body)
        {
            StatusCode = statusCode,
        };
    }
""".trimIndent()

internal fun renderReadHeaderHelper(): String = """
    private string? ReadHeader(string headerName)
    {
        return Request.Headers.TryGetValue(headerName, out var values)
            ? values.ToString()
            : null;
    }
""".trimIndent()

private fun renderResponseSwitchArms(endpoint: ResolvedDotnetAspEndpoint): String =
    endpoint.responses.joinToString(",\n") { response ->
        val variantType = resultVariantTypeName(endpoint, response)
        val headers = responseHeadersArguments(response)
        if (headers == null) {
            "$variantType response => Respond(response.Body, ${response.statusCode})"
        } else {
            "$variantType response => Respond(response.Body, ${response.statusCode}, $headers)"
        }
    }

private fun responseHeadersArguments(response: ResolvedDotnetAspResponse): String? =
    response.headers.takeIf(List<*>::isNotEmpty)?.joinToString(", ") { header ->
        "(${dotnetAspRouteLiteral(header.name)}, " +
            "response.${dotnetAspHeaderPropertyName(header.name)})"
    }

private fun renderUnsupportedResultArm(endpoint: ResolvedDotnetAspEndpoint): String = """
    _ => throw new InvalidOperationException(
        "Unsupported ${endpoint.operationName} result type '${'$'}{result.GetType().FullName}'."
    )
""".trimIndent()
