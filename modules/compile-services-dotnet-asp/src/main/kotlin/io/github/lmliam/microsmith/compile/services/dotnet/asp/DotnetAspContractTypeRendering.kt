package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpAutoProperty
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpNullableType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpParameter
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField

internal fun renderRecordType(typeName: String, fields: List<DotnetField>): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.RECORD,
    name = typeName,
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
    members = fields.map(::renderModelProperty),
)

private fun renderModelProperty(field: DotnetField): CSharp.Property = csharpAutoProperty(
    type = csharpType(renderDotnetAspModelPropertyType(field.type)),
    name = dotnetAspPascalIdentifier(field.name),
    modifiers = listOf(CSharp.Modifier.PUBLIC),
    initializer = renderDotnetAspInitializer(field.type).asInitializerExpression(),
)

internal fun renderRequestBindingType(binding: DotnetAspRequestBindingArtifact): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.RECORD,
    name = binding.typeName,
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
    members = binding.fields.map(::renderRequestBindingProperty),
)

private fun renderRequestBindingProperty(field: DotnetAspRequestFieldArtifact): CSharp.Property = csharpAutoProperty(
    type = renderRequestFieldType(field),
    name = dotnetAspPascalIdentifier(field.name),
    modifiers = listOf(CSharp.Modifier.PUBLIC),
    attributes = buildList {
        if (!field.optional && field.defaultValue == null) {
            add(CSharp.attribute(BIND_REQUIRED_ATTRIBUTE))
        }
    },
    initializer = renderDotnetAspBindingInitializer(field).asInitializerExpression(),
)

private fun renderRequestFieldType(field: DotnetAspRequestFieldArtifact): CSharp.TypeRef {
    val baseType = renderDotnetAspModelPropertyType(field.type)
    return if (field.optional && field.defaultValue == null) {
        csharpNullableType(baseType)
    } else {
        csharpType(baseType)
    }
}

internal fun renderHeadersBindingType(binding: DotnetAspHeadersBindingArtifact): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.RECORD,
    name = binding.typeName,
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
    members = binding.headers.map { header ->
        csharpAutoProperty(
            type = csharpNullableType("string"),
            name = dotnetAspPascalIdentifier(header.name),
            modifiers = listOf(CSharp.Modifier.PUBLIC),
            initializer = NULL_LITERAL,
        )
    },
)

internal fun renderResultBaseType(endpoint: DotnetAspEndpointArtifact): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.RECORD,
    name = resultBaseTypeName(endpoint),
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
)

internal fun renderResultVariantType(
    endpoint: DotnetAspEndpointArtifact,
    response: DotnetAspResponseArtifact,
): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.RECORD,
    name = resultVariantTypeName(endpoint, response),
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
    primaryConstructorParameters = buildList {
        if (response.statusCode != HTTP_NO_CONTENT_STATUS_CODE) {
            add(csharpParameter(response.model.typeName, RESULT_BODY_PROPERTY_NAME))
        }
        response.headers.forEach { header ->
            add(
                csharpParameter(
                    type = csharpNullableType("string"),
                    name = dotnetAspHeaderPropertyName(header.name),
                    defaultValue = NULL_LITERAL,
                ),
            )
        }
    },
    baseTypes = listOf(csharpType(resultBaseTypeName(endpoint))),
)

private fun String.asInitializerExpression(): String = removePrefix(" = ").removeSuffix(";")

private const val BIND_REQUIRED_ATTRIBUTE = "BindRequired"
private const val NULL_LITERAL = "null"
