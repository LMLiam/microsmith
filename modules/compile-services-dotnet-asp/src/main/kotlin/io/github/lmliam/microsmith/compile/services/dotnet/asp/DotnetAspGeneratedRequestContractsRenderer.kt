package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField

internal fun renderRequestBindingClass(binding: ResolvedDotnetAspRequestBinding): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.CLASS,
    name = binding.name,
    modifiers = listOf("public", "sealed"),
    baseTypes = emptyList(),
    attributes = emptyList(),
    primaryConstructorParameters = emptyList(),
    members = binding.fields.map(::renderRequestFieldProperty),
)

internal fun renderHeadersBindingClass(binding: ResolvedDotnetAspHeadersBinding): CSharp.Type = CSharp.Type(
    kind = CSharp.TypeKind.CLASS,
    name = binding.name,
    modifiers = listOf("public", "sealed"),
    baseTypes = emptyList(),
    attributes = emptyList(),
    primaryConstructorParameters = emptyList(),
    members = binding.headers.map { header ->
        CSharp.Property(
            type = "string?",
            name = dotnetAspPascalIdentifier(header.name),
            modifiers = listOf("public"),
            attributes = emptyList(),
            getter = "get;",
            setter = "set;",
            initializer = null,
        )
    },
)

private fun renderRequestFieldProperty(field: ResolvedDotnetAspRequestField): CSharp.Property {
    val nullable = field.optional && field.defaultValue == null
    return CSharp.Property(
        type = dotnetAspCSharpType(field.type, nullable = nullable),
        name = dotnetAspPascalIdentifier(field.name),
        modifiers = listOf("public"),
        attributes = emptyList(),
        getter = "get;",
        setter = "set;",
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
