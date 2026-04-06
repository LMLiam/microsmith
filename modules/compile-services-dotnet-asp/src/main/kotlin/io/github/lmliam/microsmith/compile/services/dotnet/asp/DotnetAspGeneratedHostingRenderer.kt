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
                    returnType = "WebApplicationBuilder",
                    modifiers = listOf("public", "static"),
                    parameters = listOf(extensionParameter("WebApplicationBuilder", "builder")),
                    body = """
                        builder.Services.AddControllers();
                        return builder;
                    """.trimIndent(),
                )
                method(
                    name = "MapMicrosmith",
                    returnType = "WebApplication",
                    modifiers = listOf("public", "static"),
                    parameters = listOf(extensionParameter("WebApplication", "app")),
                    body = """
                        app.MapControllers();
                        return app;
                    """.trimIndent(),
                )
            }
        },
    )
}
