package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality

internal fun renderActionMethod(endpoint: ResolvedDotnetAspEndpoint): CSharp.Method = CSharp.Method(
    name = endpoint.operationName,
    modifiers = DotnetAspCSharpModifiers.publicAsync,
    returnType = csharpGenericType(
        "Task",
        csharpGenericType(
            DotnetAspCSharpTypes.ACTION_RESULT,
            csharpType(resultBaseTypeName(endpoint)),
        ),
    ),
    attributes = listOf(
        csharpAttribute(
            httpMethodAttribute(endpoint.method),
            "${dotnetAspRouteLiteral(endpoint.route)}, " +
                "Name = ${dotnetAspRouteLiteral(endpoint.operationName)}",
        ),
    ),
    parameters = buildList {
        endpoint.bindings.path?.let {
            add(csharpParameter(it.name, "path", attributes = listOf(csharpAttribute("FromRoute"))))
        }
        endpoint.bindings.query?.let {
            add(csharpParameter(it.name, "query", attributes = listOf(csharpAttribute("FromQuery"))))
        }
        endpoint.bindings.body?.let {
            add(
                csharpParameter(
                    resolveBodyTypeName(endpoint),
                    "body",
                    attributes = listOf(csharpAttribute("FromBody")),
                ),
            )
        }
        add(csharpParameter(DotnetAspCSharpTypes.CANCELLATION_TOKEN, "cancellationToken"))
    },
    body = CSharp.codeBlock {
        renderHeadersPrelude(endpoint)?.let { prelude ->
            line(prelude)
            blankLine()
        }
        local(
            name = "result",
            initializer = "await On${endpoint.operationName}Async(${handlerArguments(endpoint)})",
        )
        returnStatement("Map${endpoint.operationName}Result(result)")
    },
)

internal fun renderAbstractHandler(endpoint: ResolvedDotnetAspEndpoint): CSharp.Method = CSharp.Method(
    name = "On${endpoint.operationName}Async",
    modifiers = DotnetAspCSharpModifiers.protectedAbstract,
    returnType = csharpGenericType("Task", csharpType(resultBaseTypeName(endpoint))),
    attributes = emptyList(),
    parameters = buildList {
        endpoint.bindings.path?.let { add(csharpParameter(it.name, "path")) }
        endpoint.bindings.query?.let { add(csharpParameter(it.name, "query")) }
        endpoint.bindings.headers?.let { add(csharpParameter(it.name, "headers")) }
        endpoint.bindings.body?.let { add(csharpParameter(resolveBodyTypeName(endpoint), "body")) }
        add(csharpParameter(DotnetAspCSharpTypes.CANCELLATION_TOKEN, "cancellationToken"))
    },
    body = null,
)

internal fun handlerArguments(endpoint: ResolvedDotnetAspEndpoint): String = buildList {
    endpoint.bindings.path?.let { add("path") }
    endpoint.bindings.query?.let { add("query") }
    endpoint.bindings.headers?.let { add("headers") }
    endpoint.bindings.body?.let { add("body") }
    add("cancellationToken")
}.joinToString(", ")

private fun renderHeadersPrelude(endpoint: ResolvedDotnetAspEndpoint): String? =
    endpoint.bindings.headers?.let(::renderHeadersInstantiation)

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
