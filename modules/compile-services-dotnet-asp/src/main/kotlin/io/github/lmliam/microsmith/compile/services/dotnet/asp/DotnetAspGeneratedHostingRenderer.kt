package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String = buildString {
    appendLine("using Microsoft.AspNetCore.Builder;")
    appendLine("using Microsoft.Extensions.DependencyInjection;")
    appendLine()
    appendLine("namespace ${hostingNamespace(artifact)};")
    appendLine()
    appendLine("public static class MicrosmithHostingExtensions")
    appendLine("{")
    appendLine(dotnetAspIndent(renderAddMicrosmithMethod()))
    appendLine()
    appendLine(dotnetAspIndent(renderMapMicrosmithMethod()))
    appendLine("}")
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
