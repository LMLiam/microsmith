package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp

internal fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String {
    return CSharp.render(
        CSharp.file(hostingNamespace(artifact)) {
            using(DotnetAspCSharpNamespaces.Microsoft.AspNetCore.Builder)
            using(DotnetAspCSharpNamespaces.Microsoft.Extensions.DependencyInjection)
            classType(
                name = "MicrosmithHostingExtensions",
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
            ) {
                method(
                    name = "AddMicrosmith",
                    returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplicationBuilder),
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
                    parameters = listOf(
                        extensionParameter(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplicationBuilder, "builder"),
                    ),
                    body = CSharp.codeBlock {
                        expression("builder.Services.AddControllers()")
                        returnStatement("builder")
                    },
                )
                method(
                    name = "MapMicrosmith",
                    returnType = csharpType(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplication),
                    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
                    parameters = listOf(
                        extensionParameter(DotnetAspCSharpTypes.AspNetCore.Builder.WebApplication, "app"),
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
