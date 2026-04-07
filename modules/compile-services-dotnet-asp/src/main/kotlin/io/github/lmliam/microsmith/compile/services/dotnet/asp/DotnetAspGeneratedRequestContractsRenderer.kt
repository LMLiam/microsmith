package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField

internal fun renderRequestBindingClass(binding: ResolvedDotnetAspRequestBinding): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.RECORD,
    name = binding.name,
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
    members = binding.fields.map(::renderRequestFieldProperty),
)

internal fun renderHeadersBindingClass(binding: ResolvedDotnetAspHeadersBinding): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.RECORD,
    name = binding.name,
    modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
    members = binding.headers.map { header ->
        csharpAutoProperty(
            type = csharpNullableType(DotnetAspCSharpTypes.Primitives.String),
            name = dotnetAspPascalIdentifier(header.name),
            modifiers = listOf(CSharp.Modifier.PUBLIC),
        )
    },
)

private fun renderRequestFieldProperty(field: ResolvedDotnetAspRequestField): CSharp.Property {
    val nullable = field.optional && field.defaultValue == null
    return csharpAutoProperty(
        type = csharpType(dotnetAspCSharpType(field.type, nullable = nullable)),
        name = dotnetAspPascalIdentifier(field.name),
        modifiers = listOf(CSharp.Modifier.PUBLIC),
        initializer = requestFieldInitializer(field, nullable),
    )
}

private fun requestFieldInitializer(field: ResolvedDotnetAspRequestField, nullable: Boolean): String? {
    val defaultValue = field.defaultValue
    return when {
        defaultValue != null -> dotnetAspLiteral(field.type, defaultValue)

        nullable -> null

        field.type is DotnetFieldType.String ||
            field.type is DotnetFieldType.Reference -> "null!"

        else -> null
    }
}
