package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField

internal object DotnetAspContractFileRenderer {
    fun renderServiceModelsFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("using System;")
        appendLine()
        appendLine("namespace ${contractsNamespace(artifact)};")
        appendLine()
        artifact.contractModels
            .distinctBy(DotnetAspModelArtifact::typeName)
            .filter { it.locality == DotnetAspModelLocality.SHARED }
            .sortedBy(DotnetAspModelArtifact::typeName)
            .forEachIndexed { index, model ->
                if (index > 0) {
                    appendLine()
                }
                append(renderRecordType(model.typeName, model.model.fields))
            }
    }

    fun renderRequestModelsFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("using System;")
        appendLine("using Microsoft.AspNetCore.Mvc.ModelBinding;")
        appendLine()
        appendLine("namespace ${contractsNamespace(artifact)};")
        appendLine()
        val elements = buildList {
            collectRequestBindings(artifact).forEach { add(renderRequestBindingType(it)) }
            collectHeaderBindings(artifact).forEach { add(renderHeadersBindingType(it)) }
            artifact.endpoints.forEach { endpoint ->
                endpoint.bindings.body
                    ?.takeIf { it.locality == DotnetAspModelLocality.INLINE }
                    ?.let { add(renderRecordType(it.typeName, it.model.fields)) }
            }
        }.distinct()
        elements.forEachIndexed { index, typeBlock ->
            if (index > 0) {
                appendLine()
            }
            append(typeBlock)
        }
    }

    fun renderResponseModelsFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("using System;")
        appendLine()
        appendLine("namespace ${contractsNamespace(artifact)};")
        appendLine()
        val elements = buildList {
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
        }.distinct()
        elements.forEachIndexed { index, typeBlock ->
            if (index > 0) {
                appendLine()
            }
            append(typeBlock)
        }
    }

    private fun renderRecordType(typeName: String, fields: List<DotnetField>): String = buildString {
        appendLine("public sealed record $typeName")
        appendLine("{")
        fields.forEach { field ->
            val propertyType = renderDotnetAspModelPropertyType(field.type)
            val propertyName = dotnetAspPascalIdentifier(field.name)
            val initializer = renderDotnetAspInitializer(field.type)
            appendLine("    public $propertyType $propertyName { get; set; }$initializer")
        }
        appendLine("}")
    }

    private fun renderRequestBindingType(binding: DotnetAspRequestBindingArtifact): String = buildString {
        appendLine("public sealed record ${binding.typeName}")
        appendLine("{")
        binding.fields.forEach { field ->
            if (!field.optional && field.defaultValue == null) {
                appendLine("    [BindRequired]")
            }
            val propertyType = renderDotnetAspBindingPropertyType(field)
            val propertyName = dotnetAspPascalIdentifier(field.name)
            val initializer = renderDotnetAspBindingInitializer(field)
            appendLine("    public $propertyType $propertyName { get; set; }$initializer")
        }
        appendLine("}")
    }

    private fun renderHeadersBindingType(binding: DotnetAspHeadersBindingArtifact): String = buildString {
        appendLine("public sealed record ${binding.typeName}")
        appendLine("{")
        binding.headers.forEach { header ->
            appendLine("    public string? ${dotnetAspPascalIdentifier(header.name)} { get; set; } = null;")
        }
        appendLine("}")
    }

    private fun renderResultBaseType(endpoint: DotnetAspEndpointArtifact): String =
        "public abstract record ${resultBaseTypeName(endpoint)};"

    private fun renderResultVariantType(
        endpoint: DotnetAspEndpointArtifact,
        response: DotnetAspResponseArtifact,
    ): String = buildString {
        append("public sealed record ${resultVariantTypeName(endpoint, response)}(")
        append(responseParameters(response).joinToString(", "))
        append(") : ${resultBaseTypeName(endpoint)};")
    }

    private fun responseParameters(response: DotnetAspResponseArtifact): List<String> = buildList {
        if (response.statusCode != HTTP_NO_CONTENT_STATUS_CODE) {
            add("${response.model.typeName} $RESULT_BODY_PROPERTY_NAME")
        }
        response.headers.forEach { header ->
            add("string? ${dotnetAspHeaderPropertyName(header.name)} = null")
        }
    }
}
