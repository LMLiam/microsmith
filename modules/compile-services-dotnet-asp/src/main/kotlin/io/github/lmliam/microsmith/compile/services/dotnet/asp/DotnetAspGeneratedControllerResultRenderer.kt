package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal fun renderResultMapper(endpoint: ResolvedDotnetAspEndpoint): CSharp.Method = CSharp.Method(
    name = "Map${endpoint.operationName}Result",
    modifiers = listOf("private"),
    returnType = "IActionResult",
    attributes = emptyList(),
    parameters = listOf(csharpParameter(resultBaseTypeName(endpoint), "result")),
    body = """
        return result switch
        {
            ${renderResponseSwitchArms(endpoint)},
            ${renderUnsupportedResultArm(endpoint)}
        };
    """.trimIndent(),
)

internal fun renderRespondHelper(): CSharp.Method = CSharp.Method(
    name = "Respond",
    modifiers = listOf("private"),
    returnType = "IActionResult",
    attributes = emptyList(),
    parameters = listOf(
        csharpParameter("object", "body"),
        csharpParameter("int", "statusCode"),
        csharpParameter("(string Name, string? Value)[]", "headers", modifiers = listOf("params")),
    ),
    body = """
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
    """.trimIndent(),
)

internal fun renderReadHeaderHelper(): CSharp.Method = CSharp.Method(
    name = "ReadHeader",
    modifiers = listOf("private"),
    returnType = "string?",
    attributes = emptyList(),
    parameters = listOf(csharpParameter("string", "headerName")),
    body = """
        return Request.Headers.TryGetValue(headerName, out var values)
            ? values.ToString()
            : null;
    """.trimIndent(),
)

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
