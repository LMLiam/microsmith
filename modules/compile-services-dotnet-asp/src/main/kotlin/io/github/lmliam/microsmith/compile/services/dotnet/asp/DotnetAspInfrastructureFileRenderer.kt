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
