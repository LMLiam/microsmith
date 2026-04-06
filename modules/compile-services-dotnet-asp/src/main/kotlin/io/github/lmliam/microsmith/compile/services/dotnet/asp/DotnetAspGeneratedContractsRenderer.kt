package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality

internal fun renderSharedModelsFile(artifact: DotnetAspServiceArtifact): String? {
    if (artifact.models.isEmpty()) {
        return null
    }

    val models = artifact.models.values.sortedBy(DotnetModel::name).joinToString("\n\n") { model ->
        renderModelClass(model.name, model.fields)
    }

    return buildContractsFile(artifact, models)
}

internal fun renderRequestModelsFile(artifact: DotnetAspServiceArtifact): String? {
    val requestTypes = buildList {
        artifact.rest.endpoints.forEach { endpoint ->
            endpoint.bindings.path?.let { add(renderRequestBindingClass(it)) }
            endpoint.bindings.query?.let { add(renderRequestBindingClass(it)) }
            endpoint.bindings.headers?.let { add(renderHeadersBindingClass(it)) }
            endpoint.bindings.body
                ?.takeIf { it.locality == ResolvedDotnetAspModelLocality.INLINE }
                ?.let { add(renderModelClass(inlineBodyTypeName(endpoint), it.model.fields)) }
        }
    }.distinct()

    return requestTypes.takeIf(List<String>::isNotEmpty)?.joinToString("\n\n")?.let { body ->
        buildContractsFile(artifact, body)
    }
}

internal fun renderResponseModelsFile(artifact: DotnetAspServiceArtifact): String? {
    val endpoints = artifact.rest.endpoints
    if (endpoints.isEmpty()) {
        return null
    }

    val inlineResponseModels = buildList {
        endpoints.forEach { endpoint ->
            endpoint.responses
                .filter { it.model.locality == ResolvedDotnetAspModelLocality.INLINE }
                .forEach { response ->
                    add(
                        renderModelClass(
                            inlineResponseTypeName(endpoint, response),
                            response.model.model.fields,
                        ),
                    )
                }
        }
    }.distinct()

    val sections = buildList {
        if (inlineResponseModels.isNotEmpty()) {
            add(inlineResponseModels.joinToString("\n\n"))
        }
        add(endpoints.joinToString("\n\n", transform = ::renderOperationResultTypes))
    }

    return buildContractsFile(artifact, sections.joinToString("\n\n"))
}

internal fun buildContractsFile(artifact: DotnetAspServiceArtifact, body: String): String = buildString {
    appendLine("using System;")
    appendLine()
    appendLine("namespace ${contractsNamespace(artifact)};")
    appendLine()
    append(body)
}
