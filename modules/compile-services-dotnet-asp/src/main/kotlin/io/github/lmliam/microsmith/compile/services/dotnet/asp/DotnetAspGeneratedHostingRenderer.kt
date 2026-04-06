package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String {
    return renderCSharpFile(
        CSharpFile(
            namespace = hostingNamespace(artifact),
            usings = setOf(
                "Microsoft.AspNetCore.Builder",
                "Microsoft.Extensions.DependencyInjection",
            ),
            members = listOf(
                renderCSharpType(
                    CSharpType(
                        declaration = "public static class MicrosmithHostingExtensions",
                        members = listOf(
                            renderAddMicrosmithMethod(),
                            renderMapMicrosmithMethod(),
                        ),
                    ),
                ),
            ),
        ),
    )
}

private fun renderAddMicrosmithMethod(): String = """
    public static WebApplicationBuilder AddMicrosmith(this WebApplicationBuilder builder)
    {
        builder.Services.AddControllers();
        return builder;
    }
""".trimIndent()

private fun renderMapMicrosmithMethod(): String = """
    public static WebApplication MapMicrosmith(this WebApplication app)
    {
        app.MapControllers();
        return app;
    }
""".trimIndent()
