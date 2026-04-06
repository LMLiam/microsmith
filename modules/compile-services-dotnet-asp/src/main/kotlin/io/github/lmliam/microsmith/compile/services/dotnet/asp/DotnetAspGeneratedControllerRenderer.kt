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
            using("System")
            using("System.Threading")
            using("System.Threading.Tasks")
            using("Microsoft.AspNetCore.Mvc")
            using(contractsNamespace(artifact))
            classType(
                name = "${controllerPrefix(artifact)}ControllerBase",
                modifiers = listOf("public", "abstract"),
                baseTypes = listOf("ControllerBase"),
                attributes = listOf(CSharp.Attribute("ApiController")),
            ) {
                sections.forEach(::addMember)
            }
        },
    )
}
