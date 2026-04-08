package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

internal fun dotnetAspCSharpType(type: DotnetFieldType, nullable: Boolean = false): String {
    val baseType = type.csharpType
    return if (nullable) {
        "$baseType?"
    } else {
        baseType
    }
}

internal fun dotnetAspIndent(text: String, spaces: Int = 4): String {
    val prefix = " ".repeat(spaces)
    return text.lines().joinToString("\n") { line ->
        if (line.isBlank()) {
            line
        } else {
            prefix + line
        }
    }
}
