package io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpAttributes
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.DotnetAspCSharpNamespaces
import io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp.using
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.MICROSMITH_CONTROLLER_BASE_TYPE_NAME
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.contractsNamespace
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.controllerBaseTypeName
import io.github.lmliam.microsmith.compile.services.dotnet.asp.names.controllersNamespace
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType

internal object DotnetAspControllerFileRenderer {
    fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String =
        CSharp.render(
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
                    artifact.endpoints.forEach { endpoint -> addMember(renderActionMethod(endpoint)) }
                    artifact.endpoints.forEach { endpoint -> addMember(renderAbstractHandler(endpoint)) }
                    artifact.endpoints.forEach { endpoint -> addMember(renderResultMapper(endpoint)) }
                }
            }
        )
}

internal const val VOID_TYPE_NAME = "void"
