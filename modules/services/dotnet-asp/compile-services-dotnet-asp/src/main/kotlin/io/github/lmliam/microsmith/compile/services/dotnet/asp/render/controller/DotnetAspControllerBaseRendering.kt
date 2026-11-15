package io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller

import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.argument
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.assignment
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.binary
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.call
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.codeBlock
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.conditional
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpArrayType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpNullableType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpTupleElement
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpTupleType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.identifier
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.index
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.init
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.member
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.new
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.nullLiteral
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.outVariable

internal fun renderRespondHelper(): CSharp.Method =
    CSharp.Method(
        name = "Respond",
        modifiers = listOf(CSharp.Modifier.PROTECTED),
        returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ActionResult),
        parameters =
            listOf(
                csharpParameter(csharpNullableType(DotnetCSharpTypes.Primitives.Object), "body"),
                csharpParameter(DotnetCSharpTypes.Primitives.Int, "statusCode"),
                csharpParameter(
                    type =
                        csharpArrayType(
                            csharpTupleType(
                                csharpTupleElement(csharpType(DotnetCSharpTypes.Primitives.String), "Name"),
                                csharpTupleElement(
                                    csharpNullableType(DotnetCSharpTypes.Primitives.String),
                                    "Value",
                                ),
                            ),
                        ),
                    name = "headers",
                    modifiers = listOf(CSharp.Modifier.PARAMS),
                ),
            ),
        body =
            codeBlock {
                foreachDeconstruction("name", "value", source = identifier("headers")) {
                    ifStatement(
                        binary(
                            identifier("value"),
                            CSharp.BinaryOperator.IS_NOT,
                            nullLiteral(),
                        ),
                    ) {
                        expression(
                            assignment(
                                index(
                                    member(identifier("Response"), "Headers"),
                                    identifier("name"),
                                ),
                                identifier("value"),
                            ),
                        )
                    }
                }
                blankLine()
                ifStatement("statusCode == 204") {
                    returnStatement(
                        call(
                            identifier("StatusCode"),
                            identifier("statusCode"),
                        ),
                    )
                }
                blankLine()
                returnStatement(
                    new(
                        type = csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ObjectResult),
                        arguments = listOf(identifier("body")),
                        initializers = listOf(init("StatusCode", identifier("statusCode"))),
                    ),
                )
            },
    )

internal fun renderReadHeaderHelper(): CSharp.Method =
    CSharp.Method(
        name = "ReadHeader",
        modifiers = listOf(CSharp.Modifier.PROTECTED),
        returnType = csharpNullableType(DotnetCSharpTypes.Primitives.String),
        parameters = listOf(csharpParameter(DotnetCSharpTypes.Primitives.String, "headerName")),
        body =
            codeBlock {
                returnStatement(
                    conditional(
                        condition =
                            call(
                                callee =
                                    member(
                                        member(identifier("Request"), "Headers"),
                                        "TryGetValue",
                                    ),
                                arguments =
                                    listOf(
                                        argument(identifier("headerName")),
                                        outVariable("values"),
                                    ),
                            ),
                        whenTrue = call(member(identifier("values"), "ToString")),
                        whenFalse = nullLiteral(),
                    ),
                )
            },
    )
