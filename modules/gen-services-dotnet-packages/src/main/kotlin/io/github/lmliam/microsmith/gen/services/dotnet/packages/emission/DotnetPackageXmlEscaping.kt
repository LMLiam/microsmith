package io.github.lmliam.microsmith.gen.services.dotnet.packages.emission

internal fun xmlEscape(value: String): String {
    return buildString {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(character)
            }
        }
    }
}
