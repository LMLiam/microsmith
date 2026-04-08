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
                    attributes = listOf(CSharp.attribute("ApiController")),
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
                        ),
                        CSharp.Parameter(
                            type = CSharp.nullable(CSharp.type("string")),
                            name = "ETag",
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

        "render emits braces for empty classes and semicolons for empty records" {
            val file = CSharp.file("Platform.Api.Generated") {
                classType(
                    name = "UsersControllerBase",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                    baseTypes = listOf(CSharp.type("ControllerBase")),
                )
                recordType(
                    name = "GetUserResult",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                )
            }

            CSharp.render(file).trim() shouldBe """
                namespace Platform.Api.Generated;

                public abstract class UsersControllerBase : ControllerBase
                {}

                public abstract record GetUserResult;
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
                            ),
                        ),
                        body = CSharp.codeBlock {
                            foreach("header", CSharp.identifier("headers")) {
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
                            ),
                            CSharp.Parameter(
                                type = CSharp.type("object"),
                                name = "body",
                            ),
                        ),
                        body = CSharp.codeBlock {
                            local(
                                name = "response",
                                initializer = CSharp.new(
                                    type = CSharp.type("ObjectResult"),
                                    arguments = listOf(CSharp.identifier("body")),
                                    initializers = listOf(
                                        CSharp.init("StatusCode", CSharp.intLiteral(200)),
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

        "render emits structured attributes out arguments and throw expressions" {
            val file = CSharp.file("Platform.Api.Generated") {
                classType(
                    name = "UsersControllerBase",
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                ) {
                    method(
                        name = "GetUser",
                        returnType = CSharp.type("string?"),
                        modifiers = listOf(CSharp.Modifier.PUBLIC),
                        attributes = listOf(
                            CSharp.attribute(
                                "HttpGet",
                                CSharp.positionalArgument(CSharp.stringLiteral("/users/{id}")),
                                CSharp.namedArgument("Name", CSharp.stringLiteral("GetUser")),
                            ),
                        ),
                        parameters = listOf(
                            CSharp.Parameter(
                                type = CSharp.type("string"),
                                name = "id",
                                attributes = listOf(CSharp.attribute("FromRoute")),
                            ),
                        ),
                        body = CSharp.codeBlock {
                            ifStatement(
                                CSharp.call(
                                    callee = CSharp.member(
                                        CSharp.member(CSharp.identifier("Request"), "Headers"),
                                        "TryGetValue",
                                    ),
                                    arguments = listOf(
                                        CSharp.argument(CSharp.stringLiteral("X-Correlation-Id")),
                                        CSharp.outVariable("values"),
                                    ),
                                ),
                            ) {
                                returnStatement(
                                    CSharp.call(
                                        CSharp.member(CSharp.identifier("values"), "ToString"),
                                    ),
                                )
                            }
                            returnStatement(
                                CSharp.throwExpression(
                                    CSharp.new(
                                        type = CSharp.type("InvalidOperationException"),
                                        arguments = listOf(CSharp.stringLiteral("Missing header")),
                                    ),
                                ),
                            )
                        },
                    )
                }
            }

            CSharp.render(file).trim() shouldBe """
                namespace Platform.Api.Generated;

                public abstract class UsersControllerBase
                {
                    [HttpGet("/users/{id}", Name = "GetUser")]
                    public string? GetUser([FromRoute] string id)
                    {
                        if (Request.Headers.TryGetValue("X-Correlation-Id", out var values))
                        {
                            return values.ToString();
                        }

                        return throw new InvalidOperationException("Missing header");
                    }
                }
            """.trimIndent()
        }
    })
