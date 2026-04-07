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
    parameters = listOf(csharpParameter(resultBaseTypeName(endpoint), "result")),
    body = CSharp.codeBlock {
        returnStatement(
            CSharp.switch(subject = CSharp.identifier("result"), arms = renderResultSwitchArms(endpoint)),
        )
    },
)

internal fun renderRespondHelper(): CSharp.Method = CSharp.Method(
    name = "Respond",
    modifiers = listOf(CSharp.Modifier.PROTECTED),
    returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ObjectResult),
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
        foreachDeconstruction("name", "value", source = CSharp.identifier("headers")) {
            ifStatement(
                CSharp.binary(
                    CSharp.identifier("value"),
                    CSharp.BinaryOperator.IS_NOT,
                    CSharp.nullLiteral(),
                ),
            ) {
                expression(
                    CSharp.assignment(
                        CSharp.index(
                            CSharp.member(CSharp.identifier("Response"), "Headers"),
                            CSharp.identifier("name"),
                        ),
                        CSharp.identifier("value"),
                    ),
                )
            }
        }
        blankLine()
        returnStatement(
            CSharp.new(
                type = csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ObjectResult),
                arguments = listOf(CSharp.identifier("body")),
                initializers = listOf(
                    CSharp.init(
                        memberName = "StatusCode",
                        value = CSharp.identifier("statusCode"),
                    ),
                ),
            ),
        )
    },
)

internal fun renderReadHeaderHelper(): CSharp.Method = CSharp.Method(
    name = "ReadHeader",
    modifiers = listOf(CSharp.Modifier.PROTECTED),
    returnType = csharpNullableType(DotnetAspCSharpTypes.Primitives.String),
    parameters = listOf(csharpParameter(DotnetAspCSharpTypes.Primitives.String, "headerName")),
    body = CSharp.codeBlock {
        returnStatement(
            CSharp.conditional(
                condition = CSharp.call(
                    callee = CSharp.member(
                        CSharp.member(CSharp.identifier("Request"), "Headers"),
                        "TryGetValue",
                    ),
                    arguments = listOf(
                        CSharp.argument(CSharp.identifier("headerName")),
                        CSharp.outVariable("values"),
                    ),
                ),
                whenTrue = CSharp.call(
                    CSharp.member(CSharp.identifier("values"), "ToString"),
                ),
                whenFalse = CSharp.nullLiteral(),
            ),
        )
    },
)

private fun renderResultSwitchArms(endpoint: ResolvedDotnetAspEndpoint): List<CSharp.SwitchArm> = buildList {
    addAll(renderResponseSwitchArms(endpoint))
    add(renderUnsupportedResultArm(endpoint))
}

private fun renderResponseSwitchArms(endpoint: ResolvedDotnetAspEndpoint): List<CSharp.SwitchArm> =
    endpoint.responses.map { response ->
        CSharp.switchArm(
            pattern = "${resultVariantTypeName(endpoint, response)} response",
            expression = CSharp.callValues(
                CSharp.identifier("Respond"),
                responseArguments(response),
            ),
        )
    }

private fun responseArguments(response: ResolvedDotnetAspResponse): List<CSharp.Expression> = buildList {
    add(CSharp.member(CSharp.identifier("response"), "Body"))
    add(CSharp.intLiteral(response.statusCode))
    addAll(responseHeaderArguments(response))
}

private fun responseHeaderArguments(response: ResolvedDotnetAspResponse): List<CSharp.Expression> =
    response.headers.map { header ->
        CSharp.tupleLiteral(
            CSharp.stringLiteral(header.name),
            CSharp.member(CSharp.identifier("response"), dotnetAspHeaderPropertyName(header.name)),
        )
    }

private fun renderUnsupportedResultArm(endpoint: ResolvedDotnetAspEndpoint): CSharp.SwitchArm {
    return CSharp.switchArm(
        pattern = "_",
        expression = CSharp.throwExpression(
            CSharp.new(
                type = csharpType(DotnetAspCSharpTypes.System.InvalidOperationException),
                arguments = listOf(
                    CSharp.rawExpression(
                        "\"Unsupported ${endpoint.operationName} result type '${'$'}{result.GetType().FullName}'.\"",
                    ),
                ),
            ),
        ),
    )
}
