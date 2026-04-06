package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal fun renderResultMapper(endpoint: ResolvedDotnetAspEndpoint): CSharp.Method = CSharp.Method(
    name = "Map${endpoint.operationName}Result",
    modifiers = listOf(CSharp.Modifier.PRIVATE),
    returnType = csharpGenericType(
        DotnetAspCSharpTypes.AspNetCore.Mvc.ActionResult,
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
    modifiers = listOf(CSharp.Modifier.PROTECTED),
    returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ObjectResult),
    attributes = emptyList(),
    parameters = listOf(
        csharpParameter(DotnetAspCSharpTypes.Primitives.Object, "body"),
        csharpParameter(DotnetAspCSharpTypes.Primitives.Int, "statusCode"),
        csharpParameter(
            csharpArrayType(
                csharpTupleType(
                    csharpTupleElement(csharpType(DotnetAspCSharpTypes.Primitives.String), "Name"),
                    csharpTupleElement(csharpNullableType(DotnetAspCSharpTypes.Primitives.String), "Value"),
                ),
            ),
            "headers",
            modifiers = listOf(CSharp.Modifier.PARAMS),
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
            new ${DotnetAspCSharpTypes.AspNetCore.Mvc.ObjectResult}(body)
            {
                StatusCode = statusCode,
            }
            """.trimIndent(),
        )
    },
)

internal fun renderReadHeaderHelper(): CSharp.Method = CSharp.Method(
    name = "ReadHeader",
    modifiers = listOf(CSharp.Modifier.PROTECTED),
    returnType = csharpNullableType(DotnetAspCSharpTypes.Primitives.String),
    attributes = emptyList(),
    parameters = listOf(csharpParameter(DotnetAspCSharpTypes.Primitives.String, "headerName")),
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
