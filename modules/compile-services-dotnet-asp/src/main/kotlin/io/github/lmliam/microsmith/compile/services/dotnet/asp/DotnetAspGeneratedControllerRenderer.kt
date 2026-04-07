package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp

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
            using(DotnetAspCSharpNamespaces.System.Root)
            using(DotnetAspCSharpNamespaces.System.Threading.Root)
            using(DotnetAspCSharpNamespaces.System.Threading.Tasks)
            using(DotnetAspCSharpNamespaces.Microsoft.AspNetCore.Mvc)
            using(contractsNamespace(artifact))
            classType(
                name = "${controllerPrefix(artifact)}ControllerBase",
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                baseTypes = listOf(csharpType("MicrosmithControllerBase")),
                attributes = listOf(CSharp.Attribute("ApiController")),
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
            name = "MicrosmithControllerBase",
            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
            baseTypes = listOf(csharpType(DotnetAspCSharpTypes.AspNetCore.Mvc.ControllerBase)),
        ) {
            addMember(renderRespondHelper())
            addMember(renderReadHeaderHelper())
        }
    },
)
