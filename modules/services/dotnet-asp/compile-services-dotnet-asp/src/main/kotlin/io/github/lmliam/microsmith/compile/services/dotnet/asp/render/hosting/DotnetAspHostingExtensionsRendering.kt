package io.github.lmliam.microsmith.compile.services.dotnet.asp.render.hosting

import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.call
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.codeBlock
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.extensionParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.identifier
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.member

internal fun renderAddMicrosmithExtension(): CSharp.Method =
    CSharp.Method(
        name = "AddMicrosmith",
        modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
        returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplicationBuilder),
        parameters =
            listOf(extensionParameter(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplicationBuilder, "builder")),
        body =
            codeBlock {
                expression(
                    call(
                        member(
                            member(identifier("builder"), "Services"),
                            "AddControllers",
                        ),
                    ),
                )
                returnStatement(identifier("builder"))
            },
    )

internal fun renderMapMicrosmithExtension(): CSharp.Method =
    CSharp.Method(
        name = "MapMicrosmith",
        modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
        returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplication),
        parameters = listOf(extensionParameter(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplication, "app")),
        body =
            codeBlock {
                expression(call(member(identifier("app"), "MapControllers")))
                returnStatement(identifier("app"))
            },
    )
