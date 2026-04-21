package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal object DotnetAspInfrastructureFileRenderer {
    fun renderProgramFile(artifact: DotnetAspServiceArtifact): String = """
        using ${hostingNamespace(artifact)};

        var builder = WebApplication.CreateBuilder(args);
        builder.AddMicrosmith();

        var app = builder.Build();
        app.MapMicrosmith();
        app.Run();

        public partial class Program { }
    """.trimIndent()

    fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String = """
        using Microsoft.AspNetCore.Builder;
        using Microsoft.Extensions.DependencyInjection;

        namespace ${hostingNamespace(artifact)};

        public static class MicrosmithHostingExtensions
        {
            public static WebApplicationBuilder AddMicrosmith(this WebApplicationBuilder builder)
            {
                builder.Services.AddControllers();
                return builder;
            }

            public static WebApplication MapMicrosmith(this WebApplication app)
            {
                app.MapControllers();
                return app;
            }
        }
    """.trimIndent()

    fun renderMicrosmithControllerBaseFile(artifact: DotnetAspServiceArtifact): String = """
        using Microsoft.AspNetCore.Mvc;

        namespace ${controllersNamespace(artifact)};

        public abstract class $MICROSMITH_CONTROLLER_BASE_TYPE_NAME : ControllerBase
        {
            protected ActionResult Respond(object? body, int statusCode, params (string Name, string? Value)[] headers)
            {
                foreach (var (name, value) in headers)
                {
                    if (value is not null)
                    {
                        Response.Headers[name] = value;
                    }
                }

                if (statusCode == 204)
                {
                    return StatusCode(statusCode);
                }

                return new ObjectResult(body)
                {
                    StatusCode = statusCode
                };
            }

            protected string? ReadHeader(string headerName)
            {
                return Request.Headers.TryGetValue(headerName, out var values)
                    ? values.ToString()
                    : null;
            }
        }
    """.trimIndent()
}
