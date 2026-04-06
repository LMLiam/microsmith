package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp

internal fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String? {
    val endpoints = artifact.rest.endpoints
    if (endpoints.isEmpty()) {
        return null
    }

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
        add(renderRespondHelper())
        if (endpoints.any { it.bindings.headers != null }) {
            add(renderReadHeaderHelper())
        }
    }

    return CSharp.render(
        CSharp.file(controllersNamespace(artifact)) {
            using(DotnetAspCSharpNamespaces.SYSTEM)
            using(DotnetAspCSharpNamespaces.SYSTEM_THREADING)
            using(DotnetAspCSharpNamespaces.SYSTEM_THREADING_TASKS)
            using(DotnetAspCSharpNamespaces.MICROSOFT_ASPNETCORE_MVC)
            using(contractsNamespace(artifact))
            classType(
                name = "${controllerPrefix(artifact)}ControllerBase",
                modifiers = DotnetAspCSharpModifiers.publicAbstract,
                baseTypes = listOf(csharpType(DotnetAspCSharpTypes.CONTROLLER_BASE)),
                attributes = listOf(CSharp.Attribute("ApiController")),
            ) {
                sections.forEach(::addMember)
            }
        },
    )
}
