package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpGenericType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import java.util.Locale

internal fun renderActionMethod(endpoint: DotnetAspEndpointArtifact): CSharp.Method = CSharp.Method(
    name = endpoint.operationName,
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ASYNC),
    returnType = csharpGenericType(
        DotnetCSharpTypes.Threading.Task,
        csharpGenericType(ACTION_RESULT_TYPE_NAME, csharpType(resultBaseTypeName(endpoint))),
    ),
    attributes = buildList {
        add(renderRouteAttribute(endpoint))
        endpoint.responses.forEach { response ->
            add(renderProducesResponseTypeAttribute(response))
        }
    },
    parameters = actionParameters(endpoint),
    body = CSharp.codeBlock {
        endpoint.bindings.headers?.let { binding ->
            local(name = "headers", initializer = renderHeadersInitializer(binding))
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

internal fun renderAbstractHandler(endpoint: DotnetAspEndpointArtifact): CSharp.Method = CSharp.Method(
    name = "On${endpoint.operationName}Async",
    modifiers = listOf(CSharp.Modifier.PROTECTED, CSharp.Modifier.ABSTRACT),
    returnType = csharpGenericType(
        DotnetCSharpTypes.Threading.Task,
        csharpType(resultBaseTypeName(endpoint)),
    ),
    parameters = handlerParameters(endpoint),
)

private fun renderHeadersInitializer(binding: DotnetAspHeadersBindingArtifact): CSharp.Expression = CSharp.new(
    type = csharpType(binding.typeName),
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

private fun renderRouteAttribute(endpoint: DotnetAspEndpointArtifact): CSharp.Attribute = CSharp.attribute(
    name = httpAttributeName(endpoint.method),
    CSharp.positionalArgument(CSharp.stringLiteral(endpoint.route)),
    CSharp.namedArgument("Name", CSharp.stringLiteral(endpoint.operationName)),
)

private fun renderProducesResponseTypeAttribute(response: DotnetAspResponseArtifact): CSharp.Attribute =
    CSharp.attribute(
        name = PRODUCES_RESPONSE_TYPE_ATTRIBUTE,
        CSharp.positionalArgument(CSharp.rawExpression("typeof(${responseAttributeType(response)})")),
        CSharp.positionalArgument(CSharp.intLiteral(response.statusCode)),
    )

private fun actionParameters(endpoint: DotnetAspEndpointArtifact): List<CSharp.Parameter> = buildList {
    endpoint.bindings.path?.let {
        add(
            csharpParameter(
                type = it.typeName,
                name = "path",
                attributes = listOf(CSharp.attribute(FROM_ROUTE_ATTRIBUTE)),
            ),
        )
    }
    endpoint.bindings.query?.let {
        add(
            csharpParameter(
                type = it.typeName,
                name = "query",
                attributes = listOf(CSharp.attribute(FROM_QUERY_ATTRIBUTE)),
            ),
        )
    }
    endpoint.bindings.body?.let {
        add(
            csharpParameter(
                type = it.typeName,
                name = "body",
                attributes = listOf(CSharp.attribute(FROM_BODY_ATTRIBUTE)),
            ),
        )
    }
    add(csharpParameter(DotnetCSharpTypes.Threading.CancellationToken, "cancellationToken"))
}

private fun handlerParameters(endpoint: DotnetAspEndpointArtifact): List<CSharp.Parameter> = buildList {
    endpoint.bindings.path?.let { add(csharpParameter(it.typeName, "path")) }
    endpoint.bindings.query?.let { add(csharpParameter(it.typeName, "query")) }
    endpoint.bindings.headers?.let { add(csharpParameter(it.typeName, "headers")) }
    endpoint.bindings.body?.let { add(csharpParameter(it.typeName, "body")) }
    add(csharpParameter(DotnetCSharpTypes.Threading.CancellationToken, "cancellationToken"))
}

private fun handlerArguments(endpoint: DotnetAspEndpointArtifact): List<CSharp.Expression> = buildList {
    endpoint.bindings.path?.let { add(CSharp.identifier("path")) }
    endpoint.bindings.query?.let { add(CSharp.identifier("query")) }
    endpoint.bindings.headers?.let { add(CSharp.identifier("headers")) }
    endpoint.bindings.body?.let { add(CSharp.identifier("body")) }
    add(CSharp.identifier("cancellationToken"))
}

private fun responseAttributeType(response: DotnetAspResponseArtifact): String =
    if (response.statusCode == HTTP_NO_CONTENT_STATUS_CODE) VOID_TYPE_NAME else response.model.typeName

private fun httpAttributeName(method: String): String =
    "Http" + method.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase)
