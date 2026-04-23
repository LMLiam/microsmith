package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpArrayType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpNullableType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpTupleElement
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpTupleType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType

internal fun renderRespondHelper(): CSharp.Method = CSharp.Method(
    name = "Respond",
    modifiers = listOf(CSharp.Modifier.PROTECTED),
    returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ActionResult),
    parameters = listOf(
        csharpParameter(csharpNullableType("object"), "body"),
        csharpParameter("int", "statusCode"),
        csharpParameter(
            type = csharpArrayType(
                csharpTupleType(
                    csharpTupleElement(csharpType("string"), "Name"),
                    csharpTupleElement(csharpNullableType("string"), "Value"),
                ),
            ),
            name = "headers",
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
        ifStatement("statusCode == 204") {
            returnStatement(
                CSharp.call(
                    CSharp.identifier("StatusCode"),
                    CSharp.identifier("statusCode"),
                ),
            )
        }
        blankLine()
        returnStatement(
            CSharp.new(
                type = csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ObjectResult),
                arguments = listOf(CSharp.identifier("body")),
                initializers = listOf(
                    CSharp.init("StatusCode", CSharp.identifier("statusCode")),
                ),
            ),
        )
    },
)

internal fun renderReadHeaderHelper(): CSharp.Method = CSharp.Method(
    name = "ReadHeader",
    modifiers = listOf(CSharp.Modifier.PROTECTED),
    returnType = csharpNullableType("string"),
    parameters = listOf(csharpParameter("string", "headerName")),
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
