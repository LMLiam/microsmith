package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp

internal fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String {
    return CSharp.render(
        CSharp.file(hostingNamespace(artifact)) {
            using("Microsoft.AspNetCore.Builder")
            using("Microsoft.Extensions.DependencyInjection")
            classType(
                name = "MicrosmithHostingExtensions",
                modifiers = listOf("public", "static"),
            ) {
                method(
                    name = "AddMicrosmith",
                    returnType = csharpType("WebApplicationBuilder"),
                    modifiers = listOf("public", "static"),
                    parameters = listOf(extensionParameter("WebApplicationBuilder", "builder")),
                    body = CSharp.codeBlock {
                        expression("builder.Services.AddControllers()")
                        returnStatement("builder")
                    },
                )
                method(
                    name = "MapMicrosmith",
                    returnType = csharpType("WebApplication"),
                    modifiers = listOf("public", "static"),
                    parameters = listOf(extensionParameter("WebApplication", "app")),
                    body = CSharp.codeBlock {
                        expression("app.MapControllers()")
                        returnStatement("app")
                    },
                )
            }
        },
    )
}
