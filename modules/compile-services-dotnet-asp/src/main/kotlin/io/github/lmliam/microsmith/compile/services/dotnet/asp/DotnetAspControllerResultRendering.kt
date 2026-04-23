package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpGenericType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType

internal fun renderResultMapper(endpoint: DotnetAspEndpointArtifact): CSharp.Method = CSharp.Method(
    name = "Map${endpoint.operationName}Result",
    modifiers = listOf(CSharp.Modifier.PRIVATE),
    returnType = csharpGenericType(
        DotnetAspCSharpTypes.AspNetCore.Mvc.ActionResult,
        csharpType(resultBaseTypeName(endpoint)),
    ),
    parameters = listOf(csharpParameter(resultBaseTypeName(endpoint), "result")),
    body = CSharp.codeBlock {
        returnStatement(
            CSharp.switch(
                subject = CSharp.identifier("result"),
                arms = buildList {
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
): CSharp.SwitchArm = CSharp.switchArm(
    pattern = "${resultVariantTypeName(endpoint, response)} response",
    expression = CSharp.callValues(CSharp.identifier("Respond"), responseArguments(response)),
)

private fun responseArguments(response: DotnetAspResponseArtifact): List<CSharp.Expression> = buildList {
    add(
        if (response.statusCode == HTTP_NO_CONTENT_STATUS_CODE) {
            CSharp.nullLiteral()
        } else {
            CSharp.member(CSharp.identifier("response"), RESULT_BODY_PROPERTY_NAME)
        },
    )
    add(CSharp.intLiteral(response.statusCode))
    response.headers.forEach { header ->
        add(
            CSharp.tupleLiteral(
                CSharp.stringLiteral(header.name),
                CSharp.member(
                    CSharp.identifier("response"),
                    dotnetAspHeaderPropertyName(header.name),
                ),
            ),
        )
    }
}

private fun renderUnsupportedResultArm(endpoint: DotnetAspEndpointArtifact): CSharp.SwitchArm = CSharp.switchArm(
    pattern = "_",
    expression = CSharp.throwExpression(
        CSharp.new(
            type = csharpType(DotnetCSharpTypes.System.InvalidOperationException),
            arguments = listOf(
                CSharp.rawExpression(
                    """$"Unsupported ${endpoint.operationName} result type '{result.GetType().FullName}'."""",
                ),
            ),
        ),
    ),
)
