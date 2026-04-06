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
                    modifiers = listOf("public", "abstract"),
                    baseTypes = listOf("ControllerBase"),
                    attributes = listOf(CSharp.Attribute("ApiController")),
                ) {
                    property(
                        type = "string",
                        name = "Name",
                        modifiers = listOf("public"),
                        initializer = "\"Users\"",
                    )
                    method(
                        name = "GetUserAsync",
                        returnType = "Task<IActionResult>",
                        modifiers = listOf("public", "abstract"),
                        parameters = listOf(
                            CSharp.Parameter(
                                type = "CancellationToken",
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

                    public abstract Task<IActionResult> GetUserAsync(CancellationToken cancellationToken);
                }
            """.trimIndent()
        }

        "render emits records with constructor parameters and defaults" {
            val file = CSharp.file("Platform.Api.Generated.Contracts") {
                recordType(
                    name = "GetUserResult",
                    modifiers = listOf("public", "abstract"),
                )
                recordType(
                    name = "GetUserOk",
                    modifiers = listOf("public", "sealed"),
                    primaryConstructorParameters = listOf(
                        CSharp.Parameter(
                            type = "GetUserBody",
                            name = "Body",
                            modifiers = emptyList(),
                            attributes = emptyList(),
                            defaultValue = null,
                        ),
                        CSharp.Parameter(
                            type = "string?",
                            name = "ETag",
                            modifiers = emptyList(),
                            attributes = emptyList(),
                            defaultValue = "null",
                        ),
                    ),
                    baseTypes = listOf("GetUserResult"),
                )
            }

            CSharp.render(file).trim() shouldBe """
                namespace Platform.Api.Generated.Contracts;

                public abstract record GetUserResult;

                public sealed record GetUserOk(GetUserBody Body, string? ETag = null) : GetUserResult;
            """.trimIndent()
        }
    })
