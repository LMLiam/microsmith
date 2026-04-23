package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.extensionParameter

internal fun renderAddMicrosmithExtension(): CSharp.Method = CSharp.Method(
    name = "AddMicrosmith",
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
    returnType = csharpType(WEB_APPLICATION_BUILDER_TYPE_NAME),
    parameters = listOf(extensionParameter(WEB_APPLICATION_BUILDER_TYPE_NAME, "builder")),
    body = CSharp.codeBlock {
        expression(
            CSharp.call(
                CSharp.member(
                    CSharp.member(CSharp.identifier("builder"), "Services"),
                    "AddControllers",
                ),
            ),
        )
        returnStatement(CSharp.identifier("builder"))
    },
)

internal fun renderMapMicrosmithExtension(): CSharp.Method = CSharp.Method(
    name = "MapMicrosmith",
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
    returnType = csharpType(WEB_APPLICATION_TYPE_NAME),
    parameters = listOf(extensionParameter(WEB_APPLICATION_TYPE_NAME, "app")),
    body = CSharp.codeBlock {
        expression(
            CSharp.call(
                CSharp.member(CSharp.identifier("app"), "MapControllers"),
            ),
        )
        returnStatement(CSharp.identifier("app"))
    },
)
