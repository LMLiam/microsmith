package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal fun renderModelClass(name: String, fields: List<DotnetField>): String = buildString {
    appendLine("public sealed class $name")
    appendLine("{")
    append(dotnetAspIndent(fields.joinToString("\n\n", transform = ::renderModelFieldProperty)))
    appendLine()
    append("}")
}

internal fun resolveResponseModelTypeName(
    endpoint: ResolvedDotnetAspEndpoint,
    response: ResolvedDotnetAspResponse,
): String {
    val typeName = when (response.model.locality) {
        ResolvedDotnetAspModelLocality.SHARED -> response.model.model.name
        ResolvedDotnetAspModelLocality.INLINE -> inlineResponseTypeName(endpoint, response)
    }
    return "$typeName Body"
}

private fun renderModelFieldProperty(field: DotnetField): String {
    val type = dotnetAspCSharpType(field.type)
    val initializer =
        if (field.type is DotnetFieldType.String || field.type is DotnetFieldType.Reference) {
            " = null!;"
        } else {
            ""
        }
    return "public $type ${dotnetAspPascalIdentifier(field.name)} { get; set; }$initializer"
}
