package io.github.lmliam.microsmith.compile.services.dotnet.csharp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CSharpTests :
    StringSpec({
        "render emits a class with structured members and sorted usings" {
            val file = CSharp.file("Platform.Api.Generated") {
                using("System.Threading.Tasks")
                using("Microsoft.AspNetCore.Mvc")
                classType(
                    name = "UsersControllerBase",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                    baseTypes = listOf(CSharp.type("ControllerBase")),
                    attributes = listOf(CSharp.Attribute("ApiController")),
                ) {
                    property(
                        type = CSharp.type("string"),
                        name = "Name",
                        modifiers = listOf(CSharp.Modifier.PUBLIC),
                        initializer = "\"Users\"",
                    )
                    method(
                        name = "GetUserAsync",
                        returnType = CSharp.genericType(
                            "Task",
                            CSharp.genericType("ActionResult", CSharp.type("User")),
                        ),
                        modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                        parameters = listOf(
                            CSharp.Parameter(
                                type = CSharp.type("CancellationToken"),
                                name = "cancellationToken",
                                modifiers = emptyList(),
                                attributes = emptyList(),
                                defaultValue = null,
                            ),
                        ),
                    )
                }
            }

            CSharp.render(file).trim() shouldBe """
                using Microsoft.AspNetCore.Mvc;
                using System.Threading.Tasks;

                namespace Platform.Api.Generated;

                [ApiController]
                public abstract class UsersControllerBase : ControllerBase
                {
                    public string Name { get; set; } = "Users";

                    public abstract Task<ActionResult<User>> GetUserAsync(CancellationToken cancellationToken);
                }
            """.trimIndent()
        }

        "render emits records with constructor parameters and defaults" {
            val file = CSharp.file("Platform.Api.Generated.Contracts") {
                recordType(
                    name = "GetUserResult",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                )
                recordType(
                    name = "GetUserOk",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                    primaryConstructorParameters = listOf(
                        CSharp.Parameter(
                            type = CSharp.type("GetUserBody"),
                            name = "Body",
                            modifiers = emptyList(),
                            attributes = emptyList(),
                            defaultValue = null,
                        ),
                        CSharp.Parameter(
                            type = CSharp.nullable(CSharp.type("string")),
                            name = "ETag",
                            modifiers = emptyList(),
                            attributes = emptyList(),
                            defaultValue = "null",
                        ),
                    ),
                    baseTypes = listOf(CSharp.type("GetUserResult")),
                )
            }

            CSharp.render(file).trim() shouldBe """
                namespace Platform.Api.Generated.Contracts;

                public abstract record GetUserResult;

                public sealed record GetUserOk(GetUserBody Body, string? ETag = null) : GetUserResult;
            """.trimIndent()
        }

        "render emits structured code blocks with control flow" {
            val file = CSharp.file("Platform.Api.Generated") {
                classType(
                    name = "Responder",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                ) {
                    method(
                        name = "Respond",
                        returnType = CSharp.type("string"),
                        modifiers = listOf(CSharp.Modifier.PUBLIC),
                        parameters = listOf(
                            CSharp.Parameter(
                                type = CSharp.array(CSharp.type("string")),
                                name = "headers",
                                modifiers = emptyList(),
                                attributes = emptyList(),
                                defaultValue = null,
                            ),
                        ),
                        body = CSharp.codeBlock {
                            foreach("var header in headers") {
                                ifStatement("header.Length > 0") {
                                    expression("Console.WriteLine(header)")
                                }
                            }
                            blankLine()
                            returnStatement("\"ok\"")
                        },
                    )
                }
            }

            CSharp.render(file).trim() shouldBe """
                namespace Platform.Api.Generated;

                public sealed class Responder
                {
                    public string Respond(string[] headers)
                    {
                        foreach (var header in headers)
                        {
                            if (header.Length > 0)
                            {
                                Console.WriteLine(header);
                            }
                        }

                        return "ok";
                    }
                }
            """.trimIndent()
        }

        "render emits structured expressions for calls object initializers and switch expressions" {
            val file = CSharp.file("Platform.Api.Generated") {
                classType(
                    name = "Responder",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                ) {
                    method(
                        name = "Map",
                        returnType = CSharp.type("ObjectResult"),
                        modifiers = listOf(CSharp.Modifier.PUBLIC),
                        parameters = listOf(
                            CSharp.Parameter(
                                type = CSharp.type("ResultBase"),
                                name = "result",
                                modifiers = emptyList(),
                                attributes = emptyList(),
                                defaultValue = null,
                            ),
                            CSharp.Parameter(
                                type = CSharp.type("object"),
                                name = "body",
                                modifiers = emptyList(),
                                attributes = emptyList(),
                                defaultValue = null,
                            ),
                        ),
                        body = CSharp.codeBlock {
                            local(
                                name = "response",
                                initializer = CSharp.new(
                                    type = CSharp.type("ObjectResult"),
                                    arguments = listOf(CSharp.identifier("body")),
                                    initializers = listOf(
                                        CSharp.init("StatusCode", CSharp.rawExpression("200")),
                                    ),
                                ),
                            )
                            blankLine()
                            returnStatement(
                                CSharp.switch(
                                    CSharp.identifier("result"),
                                    CSharp.switchArm(
                                        "OkResult ok",
                                        CSharp.call(
                                            CSharp.identifier("Respond"),
                                            CSharp.identifier("ok"),
                                        ),
                                    ),
                                    CSharp.switchArm(
                                        "_",
                                        CSharp.identifier("response"),
                                    ),
                                ),
                            )
                        },
                    )
                }
            }

            CSharp.render(file).trim() shouldBe """
                namespace Platform.Api.Generated;

                public sealed class Responder
                {
                    public ObjectResult Map(ResultBase result, object body)
                    {
                        var response = new ObjectResult(body)
                        {
                            StatusCode = 200
                        };

                        return result switch
                        {
                            OkResult ok => Respond(ok),
                            _ => response
                        };
                    }
                }
            """.trimIndent()
        }
    })
