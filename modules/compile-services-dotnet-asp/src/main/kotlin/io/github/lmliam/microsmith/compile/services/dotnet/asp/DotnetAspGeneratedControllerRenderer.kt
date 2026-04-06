package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String? {
    val endpoints = artifact.rest.endpoints
    if (endpoints.isEmpty()) {
        return null
    }

    val sections = buildList {
        add(endpoints.joinToString("\n\n", transform = ::renderActionMethod))
        add(endpoints.joinToString("\n\n", transform = ::renderAbstractHandler))
        add(endpoints.joinToString("\n\n", transform = ::renderResultMapper))
        add(renderRespondHelper())
        if (endpoints.any { it.bindings.headers != null }) {
            add(renderReadHeaderHelper())
        }
    }

    return renderCSharpFile(
        CSharpFile(
            namespace = controllersNamespace(artifact),
            usings = setOf(
                "System",
                "System.Threading",
                "System.Threading.Tasks",
                "Microsoft.AspNetCore.Mvc",
                contractsNamespace(artifact),
            ),
            members = listOf(
                renderCSharpType(
                    CSharpType(
                        declaration =
                        "public abstract class ${controllerPrefix(artifact)}ControllerBase : " +
                            "ControllerBase",
                        members = sections,
                        attributes = listOf("[ApiController]"),
                    ),
                ),
            ),
        ),
    )
}
