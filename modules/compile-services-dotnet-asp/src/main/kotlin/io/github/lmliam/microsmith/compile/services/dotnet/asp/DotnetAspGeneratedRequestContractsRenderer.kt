package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField

internal fun renderRequestBindingClass(binding: ResolvedDotnetAspRequestBinding): String {
    return renderCSharpType(
        CSharpType(
            declaration = "public sealed class ${binding.name}",
            members = binding.fields.map(::renderRequestFieldProperty),
        ),
    )
}

internal fun renderHeadersBindingClass(binding: ResolvedDotnetAspHeadersBinding): String {
    return renderCSharpType(
        CSharpType(
            declaration = "public sealed class ${binding.name}",
            members = binding.headers.map { header ->
                "public string? ${dotnetAspPascalIdentifier(header.name)} { get; set; }"
            },
        ),
    )
}

private fun renderRequestFieldProperty(field: ResolvedDotnetAspRequestField): String {
    val nullable = field.optional && field.defaultValue == null
    val type = dotnetAspCSharpType(field.type, nullable = nullable)
    val initializer = requestFieldInitializer(field, nullable)
    return "public $type ${dotnetAspPascalIdentifier(field.name)} { get; set; }$initializer"
}

private fun requestFieldInitializer(field: ResolvedDotnetAspRequestField, nullable: Boolean): String {
    val defaultValue = field.defaultValue
    return when {
        defaultValue != null -> " = ${dotnetAspLiteral(field.type, defaultValue)};"
        nullable -> ""
        field.type is DotnetFieldType.String ||
            field.type is DotnetFieldType.Reference -> " = null!;"
        else -> ""
    }
}
