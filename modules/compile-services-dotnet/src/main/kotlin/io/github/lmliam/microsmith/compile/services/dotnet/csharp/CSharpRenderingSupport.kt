package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun renderAttribute(attribute: CSharp.Attribute): String =
    attribute.arguments?.let { arguments -> "[${attribute.name}($arguments)]" }
        ?: "[${attribute.name}]"

internal fun indent(text: String, spaces: Int = 4): String {
    val padding = " ".repeat(spaces)
    return text.lineSequence().joinToString("\n") { line ->
        if (line.isEmpty()) {
            line
        } else {
            padding + line
        }
    }
}
