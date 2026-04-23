package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType

internal object DotnetAspControllerFileRenderer {
    fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String = CSharp.render(
        CSharp.file(controllersNamespace(artifact)) {
            using(contractsNamespace(artifact))
            using(DotnetAspCSharpNamespaces.Microsoft.AspNetCore.Mvc)
            using(DotnetAspCSharpNamespaces.System)
            using(DotnetAspCSharpNamespaces.SystemThreading.Root)
            using(DotnetAspCSharpNamespaces.SystemThreading.Tasks)
            classType(
                name = controllerBaseTypeName(artifact),
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                baseTypes = listOf(csharpType(MICROSMITH_CONTROLLER_BASE_TYPE_NAME)),
                attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.ApiController),
            ) {
                artifact.endpoints.forEach { endpoint ->
                    addMember(renderActionMethod(endpoint))
                }
                artifact.endpoints.forEach { endpoint ->
                    addMember(renderAbstractHandler(endpoint))
                }
                artifact.endpoints.forEach { endpoint ->
                    addMember(renderResultMapper(endpoint))
                }
            }
        },
    )
}

internal const val VOID_TYPE_NAME = "void"
