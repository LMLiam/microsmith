package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField

internal fun renderRequestBindingClass(binding: ResolvedDotnetAspRequestBinding): String = buildString {
    appendLine("public sealed class ${binding.name}")
    appendLine("{")
    append(
        dotnetAspIndent(
            binding.fields.joinToString("\n\n", transform = ::renderRequestFieldProperty),
        ),
    )
    appendLine()
    append("}")
}

internal fun renderHeadersBindingClass(binding: ResolvedDotnetAspHeadersBinding): String = buildString {
    appendLine("public sealed class ${binding.name}")
    appendLine("{")
    append(
        dotnetAspIndent(
            binding.headers.joinToString("\n\n") { header ->
                "public string? ${dotnetAspPascalIdentifier(header.name)} { get; set; }"
            },
        ),
    )
    appendLine()
    append("}")
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
