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

    return buildString {
        appendLine("using System;")
        appendLine("using System.Threading;")
        appendLine("using System.Threading.Tasks;")
        appendLine("using Microsoft.AspNetCore.Mvc;")
        appendLine("using ${contractsNamespace(artifact)};")
        appendLine()
        appendLine("namespace ${controllersNamespace(artifact)};")
        appendLine()
        appendLine("[ApiController]")
        appendLine(
            "public abstract class ${controllerPrefix(artifact)}ControllerBase : " +
                "ControllerBase",
        )
        appendLine("{")
        append(dotnetAspIndent(sections.joinToString("\n\n")))
        appendLine()
        appendLine("}")
    }
}
