package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpFileBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality

internal fun renderSharedModelsFile(artifact: DotnetAspServiceArtifact): String? {
    if (artifact.models.isEmpty()) {
        return null
    }

    return buildContractsFile(artifact) {
        artifact.models.values.sortedBy(DotnetModel::name).forEach { model ->
            addType(renderModelClass(model.name, model.fields))
        }
    }
}

internal fun renderRequestModelsFile(artifact: DotnetAspServiceArtifact): String? {
    val requestTypes = buildList<CSharp.Type> {
        artifact.rest.endpoints.forEach { endpoint ->
            endpoint.bindings.path?.let { add(renderRequestBindingClass(it)) }
            endpoint.bindings.query?.let { add(renderRequestBindingClass(it)) }
            endpoint.bindings.headers?.let { add(renderHeadersBindingClass(it)) }
            endpoint.bindings.body
                ?.takeIf { it.locality == ResolvedDotnetAspModelLocality.INLINE }
                ?.let { add(renderModelClass(inlineBodyTypeName(endpoint), it.model.fields)) }
        }
    }.distinct()

    return requestTypes.takeIf(List<CSharp.Type>::isNotEmpty)?.let { types ->
        buildContractsFile(artifact) {
            types.forEach(::addType)
        }
    }
}

internal fun renderResponseModelsFile(artifact: DotnetAspServiceArtifact): String? {
    val endpoints = artifact.rest.endpoints
    if (endpoints.isEmpty()) {
        return null
    }

    val inlineResponseModels = buildList<CSharp.Type> {
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

    return buildContractsFile(artifact) {
        if (inlineResponseModels.isNotEmpty()) {
            inlineResponseModels.forEach(::addType)
        }
        endpoints.forEach { endpoint ->
            renderOperationResultTypes(endpoint).forEach(::addType)
        }
    }
}

internal fun buildContractsFile(
    artifact: DotnetAspServiceArtifact,
    usings: Set<DotnetAspCSharpNamespace> = setOf(DotnetAspCSharpNamespaces.System.Root),
    build: CSharpFileBuilder.() -> Unit,
): String = CSharp.render(
    CSharp.file(contractsNamespace(artifact)) {
        usings.forEach(::using)
        build()
    },
)
