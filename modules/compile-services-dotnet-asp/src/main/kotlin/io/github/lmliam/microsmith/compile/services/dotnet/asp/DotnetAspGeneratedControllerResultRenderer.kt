package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal fun renderResultMapper(endpoint: ResolvedDotnetAspEndpoint): CSharp.Method = CSharp.Method(
    name = "Map${endpoint.operationName}Result",
    modifiers = DotnetAspCSharpModifiers.private,
    returnType = csharpGenericType(
        DotnetAspCSharpTypes.ACTION_RESULT,
        csharpType(resultBaseTypeName(endpoint)),
    ),
    attributes = emptyList(),
    parameters = listOf(csharpParameter(resultBaseTypeName(endpoint), "result")),
    body = CSharp.codeBlock {
        line("return result switch")
        line("{")
        line(dotnetAspIndent(renderResultSwitchArms(endpoint), spaces = 4))
        line("};")
    },
)

internal fun renderRespondHelper(): CSharp.Method = CSharp.Method(
    name = "Respond",
    modifiers = DotnetAspCSharpModifiers.private,
    returnType = csharpType(DotnetAspCSharpTypes.OBJECT_RESULT),
    attributes = emptyList(),
    parameters = listOf(
        csharpParameter(DotnetAspCSharpTypes.OBJECT, "body"),
        csharpParameter("int", "statusCode"),
        csharpParameter(
            csharpArrayType(
                csharpTupleType(
                    csharpTupleElement(csharpType(DotnetAspCSharpTypes.STRING), "Name"),
                    csharpTupleElement(csharpNullableType(DotnetAspCSharpTypes.STRING), "Value"),
                ),
            ),
            "headers",
            modifiers = DotnetAspCSharpModifiers.params,
        ),
    ),
    body = CSharp.codeBlock {
        foreach("var (name, value) in headers") {
            ifStatement("value is not null") {
                line("Response.Headers[name] = value;")
            }
        }
        blankLine()
        returnStatement(
            """
            new ObjectResult(body)
            {
                StatusCode = statusCode,
            }
            """.trimIndent(),
        )
    },
)

internal fun renderReadHeaderHelper(): CSharp.Method = CSharp.Method(
    name = "ReadHeader",
    modifiers = DotnetAspCSharpModifiers.private,
    returnType = csharpNullableType(DotnetAspCSharpTypes.STRING),
    attributes = emptyList(),
    parameters = listOf(csharpParameter(DotnetAspCSharpTypes.STRING, "headerName")),
    body = CSharp.codeBlock {
        line(
            """
            return Request.Headers.TryGetValue(headerName, out var values)
                ? values.ToString()
                : null;
            """.trimIndent(),
        )
    },
)

private fun renderResultSwitchArms(endpoint: ResolvedDotnetAspEndpoint): String = buildString {
    appendLine(renderResponseSwitchArms(endpoint) + ",")
    append(renderUnsupportedResultArm(endpoint))
}

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

private fun renderUnsupportedResultArm(endpoint: ResolvedDotnetAspEndpoint): String = buildString {
    appendLine("_ => throw new InvalidOperationException(")
    appendLine("    \"Unsupported ${endpoint.operationName} result type '${'$'}{result.GetType().FullName}'.\"")
    append(")")
}
