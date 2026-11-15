package io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.response.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.callValues
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.codeBlock
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpGenericType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.identifier
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.intLiteral
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.member
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.new
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.nullLiteral
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.rawExpression
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.stringLiteral
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.switch
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.switchArm
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.throwExpression
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.tupleLiteral
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.DotnetAspNamingPolicy
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.HTTP_NO_CONTENT_STATUS_CODE
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.RESULT_BODY_PROPERTY_NAME

@Inject
internal class DotnetAspControllerResultRendering(private val namingPolicy: DotnetAspNamingPolicy) {
    internal fun renderResultMapper(endpoint: DotnetAspEndpointArtifact): CSharp.Method =
        CSharp.Method(
            name = "Map${endpoint.operationName}Result",
            modifiers = listOf(CSharp.Modifier.PRIVATE),
            returnType =
                csharpGenericType(
                    DotnetAspCSharpTypes.AspNetCore.Mvc.ActionResult,
                    csharpType(namingPolicy.resultBaseTypeName(endpoint.operationName)),
                ),
            parameters = listOf(csharpParameter(namingPolicy.resultBaseTypeName(endpoint.operationName), "result")),
            body =
                codeBlock {
                    returnStatement(
                        switch(
                            subject = identifier("result"),
                            arms =
                                buildList {
                                    endpoint.responses.forEach { response ->
                                        add(renderResultSwitchArm(endpoint, response))
                                    }
                                    add(renderUnsupportedResultArm(endpoint))
                                },
                        ),
                    )
                },
        )

    private fun renderResultSwitchArm(
        endpoint: DotnetAspEndpointArtifact,
        response: DotnetAspResponseArtifact,
    ): CSharp.SwitchArm =
        switchArm(
            pattern = "${namingPolicy.resultVariantTypeName(endpoint.operationName, response.statusCode)} response",
            expression = callValues(identifier("Respond"), responseArguments(response)),
        )

    private fun responseArguments(response: DotnetAspResponseArtifact): List<CSharp.Expression> = buildList {
        add(
            if (response.statusCode == HTTP_NO_CONTENT_STATUS_CODE) {
                nullLiteral()
            } else {
                member(identifier("response"), RESULT_BODY_PROPERTY_NAME)
            },
        )
        add(intLiteral(response.statusCode))
        response.headers.forEach { header ->
            add(
                tupleLiteral(
                    stringLiteral(header.name),
                    member(
                        identifier("response"),
                        namingPolicy.requiredHeaderPropertyName(header.name),
                    ),
                ),
            )
        }
    }

    private fun renderUnsupportedResultArm(endpoint: DotnetAspEndpointArtifact): CSharp.SwitchArm =
        switchArm(
            pattern = "_",
            expression =
                throwExpression(
                    new(
                        type = csharpType(DotnetCSharpTypes.System.InvalidOperationException),
                        arguments =
                            listOf(
                                rawExpression(
                                    """$"Unsupported ${endpoint.operationName} result type """ +
                                        "'{result.GetType().FullName}'.\"",
                                ),
                            ),
                    ),
                ),
        )
}
