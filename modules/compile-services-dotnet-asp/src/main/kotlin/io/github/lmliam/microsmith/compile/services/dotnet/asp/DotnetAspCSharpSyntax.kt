package io.github.lmliam.microsmith.compile.services.dotnet.asp

internal data class CSharpFile(
    val namespace: String,
    val usings: Set<String> = emptySet(),
    val members: List<String>,
)

internal data class CSharpType(
    val declaration: String,
    val members: List<String>,
    val attributes: List<String> = emptyList(),
)

internal fun renderCSharpFile(file: CSharpFile): String = buildString {
    file.usings
        .sorted()
        .forEach { namespace ->
            appendLine("using $namespace;")
        }
    if (file.usings.isNotEmpty()) {
        appendLine()
    }
    appendLine("namespace ${file.namespace};")
    appendLine()
    append(file.members.joinToString("\n\n"))
}

internal fun renderCSharpType(type: CSharpType): String = buildString {
    type.attributes.forEach(::appendLine)
    appendLine(type.declaration)
    appendLine("{")
    append(dotnetAspIndent(type.members.joinToString("\n\n")))
    appendLine()
    append("}")
}
