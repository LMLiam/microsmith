package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality

internal fun renderActionMethod(endpoint: ResolvedDotnetAspEndpoint): String = buildString {
    appendLine(
        "[${httpMethodAttribute(endpoint.method)}(" +
            "${dotnetAspRouteLiteral(endpoint.route)}, " +
            "Name = ${dotnetAspRouteLiteral(endpoint.operationName)})]",
    )
    appendLine("public async Task<IActionResult> ${endpoint.operationName}(")
    append(dotnetAspIndent(renderActionSignatureParameters(endpoint), spaces = 4))
    appendLine()
    appendLine(")")
    appendLine("{")
    renderHeadersPrelude(endpoint)?.let { prelude ->
        append(dotnetAspIndent(prelude))
        appendLine()
        appendLine()
    }
    appendLine(
        dotnetAspIndent(
            "var result = await On${endpoint.operationName}Async(${handlerArguments(endpoint)});",
        ),
    )
    appendLine(dotnetAspIndent("return Map${endpoint.operationName}Result(result);"))
    append("}")
}

internal fun renderAbstractHandler(endpoint: ResolvedDotnetAspEndpoint): String = buildString {
    appendLine(
        "protected abstract Task<${resultBaseTypeName(endpoint)}> " +
            "On${endpoint.operationName}Async(",
    )
    append(dotnetAspIndent(renderHandlerParameters(endpoint), spaces = 4))
    appendLine()
    append(");")
}

private fun renderActionSignatureParameters(endpoint: ResolvedDotnetAspEndpoint): String = buildList {
    endpoint.bindings.path?.let { add("[FromRoute] ${it.name} path") }
    endpoint.bindings.query?.let { add("[FromQuery] ${it.name} query") }
    endpoint.bindings.body?.let { add("[FromBody] ${resolveBodyTypeName(endpoint)} body") }
    add("CancellationToken cancellationToken")
}.joinToString(",\n")

internal fun handlerArguments(endpoint: ResolvedDotnetAspEndpoint): String = buildList {
    endpoint.bindings.path?.let { add("path") }
    endpoint.bindings.query?.let { add("query") }
    endpoint.bindings.headers?.let { add("headers") }
    endpoint.bindings.body?.let { add("body") }
    add("cancellationToken")
}.joinToString(", ")

private fun renderHeadersPrelude(endpoint: ResolvedDotnetAspEndpoint): String? =
    endpoint.bindings.headers?.let(::renderHeadersInstantiation)

private fun renderHandlerParameters(endpoint: ResolvedDotnetAspEndpoint): String = buildList {
    endpoint.bindings.path?.let { add("${it.name} path") }
    endpoint.bindings.query?.let { add("${it.name} query") }
    endpoint.bindings.headers?.let { add("${it.name} headers") }
    endpoint.bindings.body?.let { add("${resolveBodyTypeName(endpoint)} body") }
    add("CancellationToken cancellationToken")
}.joinToString(",\n")

private fun renderHeadersInstantiation(binding: ResolvedDotnetAspHeadersBinding): String = buildString {
    appendLine("var headers = new ${binding.name}")
    appendLine("{")
    append(
        dotnetAspIndent(
            binding.headers.joinToString(",\n") { header ->
                "${dotnetAspPascalIdentifier(header.name)} = " +
                    "ReadHeader(${dotnetAspRouteLiteral(header.headerName)})"
            },
            spaces = 4,
        ),
    )
    appendLine()
    append("};")
}

internal fun resolveBodyTypeName(endpoint: ResolvedDotnetAspEndpoint): String =
    when (endpoint.bindings.body?.locality) {
        ResolvedDotnetAspModelLocality.SHARED ->
            requireNotNull(endpoint.bindings.body).model.name

        ResolvedDotnetAspModelLocality.INLINE -> inlineBodyTypeName(endpoint)

        null ->
            error(
                "ASP.NET endpoint '${endpoint.operationName}' does not declare a body binding.",
            )
    }
