package io.github.lmliam.microsmith.compile.services.dotnet.asp.render.hosting
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpNamespaces
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.using
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.MICROSMITH_CONTROLLER_BASE_TYPE_NAME
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.controllersNamespace
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.hostingNamespace
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.renderReadHeaderHelper
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.renderRespondHelper
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
            using(DotnetAspCSharpNamespaces.Microsoft.AspNetCore.Builder)
            using(DotnetAspCSharpNamespaces.Microsoft.Extensions.DependencyInjection)
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
            using(DotnetAspCSharpNamespaces.Microsoft.AspNetCore.Mvc)
            classType(
                name = MICROSMITH_CONTROLLER_BASE_TYPE_NAME,
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                baseTypes = listOf(csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ControllerBase)),
            ) {
                addMember(renderRespondHelper())
                addMember(renderReadHeaderHelper())
            }
        },
    )
}

private const val MICROSMITH_HOSTING_EXTENSIONS_TYPE_NAME = "MicrosmithHostingExtensions"
