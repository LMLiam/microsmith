package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType

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

    fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String = CSharp.render(
        CSharp.file(hostingNamespace(artifact)) {
            using(ASP_NET_BUILDER_NAMESPACE)
            using(DEPENDENCY_INJECTION_NAMESPACE)
            classType(
                name = MICROSMITH_HOSTING_EXTENSIONS_TYPE_NAME,
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC),
            ) {
                addMember(renderAddMicrosmithExtension())
                addMember(renderMapMicrosmithExtension())
            }
        },
    )

    fun renderMicrosmithControllerBaseFile(artifact: DotnetAspServiceArtifact): String = CSharp.render(
        CSharp.file(controllersNamespace(artifact)) {
            using(ASP_NET_MVC_NAMESPACE)
            classType(
                name = MICROSMITH_CONTROLLER_BASE_TYPE_NAME,
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                baseTypes = listOf(csharpType(CONTROLLER_BASE_TYPE_NAME)),
            ) {
                addMember(renderRespondHelper())
                addMember(renderReadHeaderHelper())
            }
        },
    )
}

internal const val CONTROLLER_BASE_TYPE_NAME = "ControllerBase"
internal const val CONTROLLER_ACTION_RESULT_TYPE_NAME = "ActionResult"
internal const val OBJECT_RESULT_TYPE_NAME = "ObjectResult"
internal const val WEB_APPLICATION_BUILDER_TYPE_NAME = "WebApplicationBuilder"
internal const val WEB_APPLICATION_TYPE_NAME = "WebApplication"
private const val ASP_NET_MVC_NAMESPACE = "Microsoft.AspNetCore.Mvc"
private const val ASP_NET_BUILDER_NAMESPACE = "Microsoft.AspNetCore.Builder"
private const val DEPENDENCY_INJECTION_NAMESPACE = "Microsoft.Extensions.DependencyInjection"
private const val MICROSMITH_HOSTING_EXTENSIONS_TYPE_NAME = "MicrosmithHostingExtensions"
