package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpNamespaces
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.using

internal fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String {
    val endpoints = artifact.rest.endpoints
    val sections = buildList<CSharp.Member> {
        endpoints.forEach { endpoint ->
            add(renderActionMethod(endpoint))
        }
        endpoints.forEach { endpoint ->
            add(renderAbstractHandler(endpoint))
        }
        endpoints.forEach { endpoint ->
            add(renderResultMapper(endpoint))
        }
    }

    return CSharp.render(
        CSharp.file(controllersNamespace(artifact)) {
            using(DotnetCSharpNamespaces.System.Root)
            using(DotnetCSharpNamespaces.System.Threading.Root)
            using(DotnetCSharpNamespaces.System.Threading.Tasks)
            using(DotnetAspCSharpNamespaces.Microsoft.AspNetCore.Mvc)
            using(contractsNamespace(artifact))
            classType(
                name = controllerBaseTypeName(artifact),
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                baseTypes = listOf(csharpType(MICROSMITH_CONTROLLER_BASE_TYPE_NAME)),
                attributes = listOf(DotnetAspCSharpAttributes.Microsoft.AspNetCore.Mvc.ApiController),
            ) {
                sections.forEach(::addMember)
            }
        },
    )
}

internal fun renderMicrosmithControllerBaseFile(artifact: DotnetAspServiceArtifact): String = CSharp.render(
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
