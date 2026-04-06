package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp

internal fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String {
    return CSharp.render(
        CSharp.file(hostingNamespace(artifact)) {
            using(DotnetAspCSharpNamespaces.MICROSOFT_ASPNETCORE_BUILDER)
            using(DotnetAspCSharpNamespaces.MICROSOFT_EXTENSIONS_DEPENDENCY_INJECTION)
            classType(
                name = "MicrosmithHostingExtensions",
                modifiers = DotnetAspCSharpModifiers.publicStatic,
            ) {
                method(
                    name = "AddMicrosmith",
                    returnType = csharpType(DotnetAspCSharpTypes.WEB_APPLICATION_BUILDER),
                    modifiers = DotnetAspCSharpModifiers.publicStatic,
                    parameters = listOf(
                        extensionParameter(DotnetAspCSharpTypes.WEB_APPLICATION_BUILDER, "builder"),
                    ),
                    body = CSharp.codeBlock {
                        expression("builder.Services.AddControllers()")
                        returnStatement("builder")
                    },
                )
                method(
                    name = "MapMicrosmith",
                    returnType = csharpType(DotnetAspCSharpTypes.WEB_APPLICATION),
                    modifiers = DotnetAspCSharpModifiers.publicStatic,
                    parameters = listOf(
                        extensionParameter(DotnetAspCSharpTypes.WEB_APPLICATION, "app"),
                    ),
                    body = CSharp.codeBlock {
                        expression("app.MapControllers()")
                        returnStatement("app")
                    },
                )
            }
        },
    )
}
