package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal fun renderModelClass(name: String, fields: List<DotnetField>): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.CLASS,
    name = name,
    modifiers = DotnetAspCSharpModifiers.publicSealed,
    baseTypes = emptyList(),
    attributes = emptyList(),
    primaryConstructorParameters = emptyList(),
    members = fields.map(::renderModelFieldProperty),
)

internal fun resolveResponseModelTypeName(
    endpoint: ResolvedDotnetAspEndpoint,
    response: ResolvedDotnetAspResponse,
): String {
    return when (response.model.locality) {
        ResolvedDotnetAspModelLocality.SHARED -> response.model.model.name
        ResolvedDotnetAspModelLocality.INLINE -> inlineResponseTypeName(endpoint, response)
    }
}

private fun renderModelFieldProperty(field: DotnetField): CSharp.Property = CSharp.Property(
    type = csharpType(dotnetAspCSharpType(field.type)),
    name = dotnetAspPascalIdentifier(field.name),
    modifiers = DotnetAspCSharpModifiers.public,
    attributes = emptyList(),
    getter = "get;",
    setter = "set;",
    initializer =
    if (field.type is DotnetFieldType.String || field.type is DotnetFieldType.Reference) {
        "null!"
    } else {
        null
    },
)
