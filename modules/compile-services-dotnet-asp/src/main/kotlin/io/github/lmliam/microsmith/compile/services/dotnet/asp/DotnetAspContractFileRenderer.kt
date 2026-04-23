package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpFileBuilder

internal object DotnetAspContractFileRenderer {
    fun renderServiceModelsFile(artifact: DotnetAspServiceArtifact): String =
        renderContractsFile(artifact, usings = setOf(DotnetAspCSharpNamespaces.System)) {
            artifact.contractModels
                .distinctBy(DotnetAspModelArtifact::typeName)
                .filter { it.locality == DotnetAspModelLocality.SHARED }
                .sortedBy(DotnetAspModelArtifact::typeName)
                .forEach { addType(renderRecordType(it.typeName, it.model.fields)) }
        }

    fun renderRequestModelsFile(artifact: DotnetAspServiceArtifact): String = renderContractsFile(
        artifact,
        usings = setOf(
            DotnetAspCSharpNamespaces.System,
            DotnetAspCSharpNamespaces.Microsoft.AspNetCore.ModelBinding,
        ),
    ) {
        buildList {
            collectRequestBindings(artifact).forEach { add(renderRequestBindingType(it)) }
            collectHeaderBindings(artifact).forEach { add(renderHeadersBindingType(it)) }
            artifact.endpoints.forEach { endpoint ->
                endpoint.bindings.body
                    ?.takeIf { it.locality == DotnetAspModelLocality.INLINE }
                    ?.let { add(renderRecordType(it.typeName, it.model.fields)) }
            }
        }.distinctBy(CSharp.Type::name)
            .forEach(::addType)
    }

    fun renderResponseModelsFile(artifact: DotnetAspServiceArtifact): String =
        renderContractsFile(artifact, usings = setOf(DotnetAspCSharpNamespaces.System)) {
            buildList {
                artifact.endpoints.forEach { endpoint ->
                    endpoint.responses
                        .map(DotnetAspResponseArtifact::model)
                        .filter { it.locality == DotnetAspModelLocality.INLINE }
                        .distinctBy(DotnetAspModelArtifact::typeName)
                        .forEach { model -> add(renderRecordType(model.typeName, model.model.fields)) }
                }
                artifact.endpoints.forEach { endpoint ->
                    add(renderResultBaseType(endpoint))
                    endpoint.responses.forEach { response ->
                        add(renderResultVariantType(endpoint, response))
                    }
                }
            }.distinctBy(CSharp.Type::name)
                .forEach(::addType)
        }

    private fun renderContractsFile(
        artifact: DotnetAspServiceArtifact,
        usings: Set<io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpNamespace>,
        build: CSharpFileBuilder.() -> Unit,
    ): String = CSharp.render(
        CSharp.file(contractsNamespace(artifact)) {
            usings.forEach(::using)
            build()
        },
    )
}
