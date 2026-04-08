package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpGenericType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality

internal fun renderActionMethod(endpoint: ResolvedDotnetAspEndpoint): CSharp.Method = CSharp.Method(
    name = endpoint.operationName,
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ASYNC),
    returnType = csharpGenericType(
        DotnetCSharpTypes.Threading.Task,
        csharpGenericType(
            DotnetAspCSharpTypes.AspNetCore.Mvc.ActionResult,
            csharpType(resultBaseTypeName(endpoint)),
        ),
    ),
    attributes = listOf(
        DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.endpointRoute(
            method = endpoint.method,
            route = endpoint.route,
            operationName = endpoint.operationName,
        ),
    ),
    parameters = buildList {
        endpoint.bindings.path?.let {
            add(
                csharpParameter(
                    it.name,
                    "path",
                    attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.FromRoute),
                ),
            )
        }
        endpoint.bindings.query?.let {
            add(
                csharpParameter(
                    it.name,
                    "query",
                    attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.FromQuery),
                ),
            )
        }
        endpoint.bindings.body?.let {
            add(
                csharpParameter(
                    resolveBodyTypeName(endpoint),
                    "body",
                    attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.FromBody),
                ),
            )
        }
        add(csharpParameter(DotnetCSharpTypes.Threading.CancellationToken, "cancellationToken"))
    },
    body = CSharp.codeBlock {
        renderHeadersPrelude(endpoint)?.let { headerInitializer ->
            local(name = "headers", initializer = headerInitializer)
            blankLine()
        }
        local(
            name = "result",
            initializer = CSharp.await(
                CSharp.callValues(
                    CSharp.identifier("On${endpoint.operationName}Async"),
                    handlerArguments(endpoint),
                ),
            ),
        )
        returnStatement(
            CSharp.call(
                CSharp.identifier("Map${endpoint.operationName}Result"),
                CSharp.identifier("result"),
            ),
        )
    },
)

internal fun renderAbstractHandler(endpoint: ResolvedDotnetAspEndpoint): CSharp.Method = CSharp.Method(
    name = "On${endpoint.operationName}Async",
    modifiers = listOf(CSharp.Modifier.PROTECTED, CSharp.Modifier.ABSTRACT),
    returnType = csharpGenericType(DotnetCSharpTypes.Threading.Task, csharpType(resultBaseTypeName(endpoint))),
    parameters = buildList {
        endpoint.bindings.path?.let { add(csharpParameter(it.name, "path")) }
        endpoint.bindings.query?.let { add(csharpParameter(it.name, "query")) }
        endpoint.bindings.headers?.let { add(csharpParameter(it.name, "headers")) }
        endpoint.bindings.body?.let { add(csharpParameter(resolveBodyTypeName(endpoint), "body")) }
        add(csharpParameter(DotnetCSharpTypes.Threading.CancellationToken, "cancellationToken"))
    },
)

internal fun handlerArguments(endpoint: ResolvedDotnetAspEndpoint): List<CSharp.Expression> = buildList {
    endpoint.bindings.path?.let { add(CSharp.identifier("path")) }
    endpoint.bindings.query?.let { add(CSharp.identifier("query")) }
    endpoint.bindings.headers?.let { add(CSharp.identifier("headers")) }
    endpoint.bindings.body?.let { add(CSharp.identifier("body")) }
    add(CSharp.identifier("cancellationToken"))
}

private fun renderHeadersPrelude(endpoint: ResolvedDotnetAspEndpoint): CSharp.Expression? =
    endpoint.bindings.headers?.let(::renderHeadersInstantiation)

private fun renderHeadersInstantiation(binding: ResolvedDotnetAspHeadersBinding): CSharp.Expression = CSharp.new(
    type = csharpType(binding.name),
    initializers = binding.headers.map { header ->
        CSharp.init(
            memberName = dotnetAspPascalIdentifier(header.name),
            value = CSharp.call(
                CSharp.identifier("ReadHeader"),
                CSharp.stringLiteral(header.headerName),
            ),
        )
    },
)

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
