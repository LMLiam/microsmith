package io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.response.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpAttributes
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.endpointRoute
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.producesResponseType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.await
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.call
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.callValues
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.codeBlock
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpGenericType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.identifier
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.init
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.new
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.stringLiteral
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.DotnetAspNamingPolicy
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.HTTP_NO_CONTENT_STATUS_CODE

@Inject
internal class DotnetAspControllerActionRendering(private val namingPolicy: DotnetAspNamingPolicy) {
    internal fun renderActionMethod(endpoint: DotnetAspEndpointArtifact): CSharp.Method =
        CSharp.Method(
            name = endpoint.operationName,
            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ASYNC),
            returnType =
                csharpGenericType(
                    DotnetCSharpTypes.Threading.Task,
                    csharpGenericType(
                        DotnetAspCSharpTypes.AspNetCore.Mvc.ActionResult,
                        csharpType(namingPolicy.resultBaseTypeName(endpoint.operationName)),
                    ),
                ),
            attributes =
                buildList {
                    add(renderRouteAttribute(endpoint))
                    endpoint.responses.forEach { response -> add(renderProducesResponseTypeAttribute(response)) }
                },
            parameters = actionParameters(endpoint),
            body =
                codeBlock {
                    endpoint.bindings.headers?.let { binding ->
                        local(name = "headers", initializer = renderHeadersInitializer(binding))
                        blankLine()
                    }
                    local(
                        name = "result",
                        initializer =
                            await(
                                callValues(
                                    identifier("On${endpoint.operationName}Async"),
                                    handlerArguments(endpoint),
                                ),
                            ),
                    )
                    returnStatement(
                        call(
                            identifier("Map${endpoint.operationName}Result"),
                            identifier("result"),
                        ),
                    )
                },
        )

    internal fun renderAbstractHandler(endpoint: DotnetAspEndpointArtifact): CSharp.Method =
        CSharp.Method(
            name = "On${endpoint.operationName}Async",
            modifiers = listOf(CSharp.Modifier.PROTECTED, CSharp.Modifier.ABSTRACT),
            returnType =
                csharpGenericType(
                    DotnetCSharpTypes.Threading.Task,
                    csharpType(namingPolicy.resultBaseTypeName(endpoint.operationName)),
                ),
            parameters = handlerParameters(endpoint),
        )

    private fun renderHeadersInitializer(binding: DotnetAspHeadersBindingArtifact): CSharp.Expression =
        new(
            type = csharpType(binding.typeName),
            initializers =
                binding.headers.map { header ->
                    init(
                        memberName = namingPolicy.pascalIdentifier(header.name),
                        value =
                            call(
                                identifier("ReadHeader"),
                                stringLiteral(header.headerName),
                            ),
                    )
                },
        )

    private fun renderRouteAttribute(endpoint: DotnetAspEndpointArtifact): CSharp.Attribute =
        endpointRoute(
            method = endpoint.method,
            route = endpoint.route,
            operationName = endpoint.operationName,
        )

    private fun renderProducesResponseTypeAttribute(response: DotnetAspResponseArtifact): CSharp.Attribute =
        producesResponseType(
            typeName = responseAttributeType(response),
            statusCode = response.statusCode,
        )

    private fun actionParameters(endpoint: DotnetAspEndpointArtifact): List<CSharp.Parameter> = buildList {
        endpoint.bindings.path?.let {
            add(
                csharpParameter(
                    type = it.typeName,
                    name = "path",
                    attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.FromRoute),
                ),
            )
        }
        endpoint.bindings.query?.let {
            add(
                csharpParameter(
                    type = it.typeName,
                    name = "query",
                    attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.FromQuery),
                ),
            )
        }
        endpoint.bindings.body?.let {
            add(
                csharpParameter(
                    type = it.typeName,
                    name = "body",
                    attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.FromBody),
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
        endpoint.bindings.path?.let { add(identifier("path")) }
        endpoint.bindings.query?.let { add(identifier("query")) }
        endpoint.bindings.headers?.let { add(identifier("headers")) }
        endpoint.bindings.body?.let { add(identifier("body")) }
        add(identifier("cancellationToken"))
    }

    private fun responseAttributeType(response: DotnetAspResponseArtifact): String =
        if (response.statusCode == HTTP_NO_CONTENT_STATUS_CODE) VOID_TYPE_NAME else response.model.typeName
}
