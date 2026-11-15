package io.github.lmliam.microsmith.compile.services.dotnet.csharp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val csharpRenderer = CSharpRenderer()

class CSharpTests :
    StringSpec(
        {
            "render emits a class with structured members and sorted usings" {
                val file =
                    file("Platform.Api.Generated") {
                        using("System.Threading.Tasks")
                        using("Microsoft.AspNetCore.Mvc")
                        classType(
                            name = "UsersControllerBase",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                            baseTypes = listOf(csharpType("ControllerBase")),
                            attributes = listOf(attribute("ApiController")),
                        ) {
                            property(
                                type = csharpType("string"),
                                name = "Name",
                                modifiers = listOf(CSharp.Modifier.PUBLIC),
                                initializer = "\"Users\"",
                            )
                            method(
                                name = "GetUserAsync",
                                returnType =
                                    csharpGenericType(
                                        "Task",
                                        csharpGenericType("ActionResult", csharpType("User")),
                                    ),
                                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                                parameters =
                                    listOf(
                                        CSharp.Parameter(
                                            type = csharpType("CancellationToken"),
                                            name = "cancellationToken",
                                        ),
                                    ),
                            )
                        }
                    }

                csharpRenderer.render(file).trim() shouldBe csharpResource("class-with-structured-members.cs")
            }

            "render emits records with constructor parameters and defaults" {
                val file =
                    file("Platform.Api.Generated.Contracts") {
                        recordType(
                            name = "GetUserResult",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                        )
                        recordType(
                            name = "GetUserOk",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                            primaryConstructorParameters =
                                listOf(
                                    CSharp.Parameter(
                                        type = csharpType("GetUserBody"),
                                        name = "Body",
                                    ),
                                    CSharp.Parameter(
                                        type = csharpNullableType("string"),
                                        name = "ETag",
                                        defaultValue = "null",
                                    ),
                                ),
                            baseTypes = listOf(csharpType("GetUserResult")),
                        )
                    }

                csharpRenderer.render(file).trim() shouldBe
                    """
                    namespace Platform.Api.Generated.Contracts;

                    public abstract record GetUserResult;

                    public sealed record GetUserOk(GetUserBody Body, string? ETag = null) : GetUserResult;
                    """
                        .trimIndent()
            }

            "render emits braces for empty classes and semicolons for empty records" {
                val file =
                    file("Platform.Api.Generated") {
                        classType(
                            name = "UsersControllerBase",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                            baseTypes = listOf(csharpType("ControllerBase")),
                        )
                        recordType(
                            name = "GetUserResult",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                        )
                    }

                csharpRenderer.render(file).trim() shouldBe
                    """
                    namespace Platform.Api.Generated;

                    public abstract class UsersControllerBase : ControllerBase
                    {}

                    public abstract record GetUserResult;
                    """
                        .trimIndent()
            }

            "render emits structured code blocks with control flow" {
                val file =
                    file("Platform.Api.Generated") {
                        classType(
                            name = "Responder",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                        ) {
                            method(
                                name = "Respond",
                                returnType = csharpType("string"),
                                modifiers = listOf(CSharp.Modifier.PUBLIC),
                                parameters =
                                    listOf(
                                        CSharp.Parameter(
                                            type = csharpArrayType(csharpType("string")),
                                            name = "headers",
                                        ),
                                    ),
                                body =
                                    codeBlock {
                                        foreach("header", identifier("headers")) {
                                            ifStatement("header.Length > 0") { expression("Console.WriteLine(header)") }
                                        }
                                        blankLine()
                                        returnStatement("\"ok\"")
                                    },
                            )
                        }
                    }

                csharpRenderer.render(file).trim() shouldBe
                    csharpResource("structured-code-blocks-with-control-flow.cs")
            }

            "render emits structured expressions for calls object initializers and switch expressions" {
                val file =
                    file("Platform.Api.Generated") {
                        classType(
                            name = "Responder",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                        ) {
                            method(
                                name = "Map",
                                returnType = csharpType("ObjectResult"),
                                modifiers = listOf(CSharp.Modifier.PUBLIC),
                                parameters =
                                    listOf(
                                        CSharp.Parameter(
                                            type = csharpType("ResultBase"),
                                            name = "result",
                                        ),
                                        CSharp.Parameter(
                                            type = csharpType("object"),
                                            name = "body",
                                        ),
                                    ),
                                body =
                                    codeBlock {
                                        local(
                                            name = "response",
                                            initializer =
                                                new(
                                                    type = csharpType("ObjectResult"),
                                                    arguments = listOf(identifier("body")),
                                                    initializers = listOf(init("StatusCode", intLiteral(200))),
                                                ),
                                        )
                                        blankLine()
                                        returnStatement(
                                            switch(
                                                identifier("result"),
                                                switchArm(
                                                    "OkResult ok",
                                                    call(
                                                        identifier("Respond"),
                                                        identifier("ok"),
                                                    ),
                                                ),
                                                switchArm(
                                                    "_",
                                                    identifier("response"),
                                                ),
                                            ),
                                        )
                                    },
                            )
                        }
                    }

                csharpRenderer.render(file).trim() shouldBe csharpResource("structured-expressions.cs")
            }

            "render emits structured attributes out arguments and throw expressions" {
                val file =
                    file("Platform.Api.Generated") {
                        classType(
                            name = "UsersControllerBase",
                            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                        ) {
                            method(
                                name = "GetUser",
                                returnType = csharpType("string?"),
                                modifiers = listOf(CSharp.Modifier.PUBLIC),
                                attributes =
                                    listOf(
                                        attribute(
                                            "HttpGet",
                                            positionalArgument(stringLiteral("/users/{id}")),
                                            namedArgument("Name", stringLiteral("GetUser")),
                                        ),
                                    ),
                                parameters =
                                    listOf(
                                        CSharp.Parameter(
                                            type = csharpType("string"),
                                            name = "id",
                                            attributes = listOf(attribute("FromRoute")),
                                        ),
                                    ),
                                body =
                                    codeBlock {
                                        ifStatement(
                                            call(
                                                callee =
                                                    member(
                                                        member(identifier("Request"), "Headers"),
                                                        "TryGetValue",
                                                    ),
                                                arguments =
                                                    listOf(
                                                        argument(stringLiteral("X-Correlation-Id")),
                                                        outVariable("values"),
                                                    ),
                                            ),
                                        ) {
                                            returnStatement(
                                                call(member(identifier("values"), "ToString")),
                                            )
                                        }
                                        returnStatement(
                                            throwExpression(
                                                new(
                                                    type = csharpType("InvalidOperationException"),
                                                    arguments = listOf(stringLiteral("Missing header")),
                                                ),
                                            ),
                                        )
                                    },
                            )
                        }
                    }

                csharpRenderer.render(file).trim() shouldBe
                    csharpResource("structured-attributes-out-arguments-and-throw.cs")
            }
        },
    )

private fun csharpResource(name: String): String {
    val resource = "/csharp/$name"
    return checkNotNull(CSharpTests::class.java.getResource(resource)) { "Missing test resource $resource" }
        .readText()
        .trimEnd()
}
