package io.github.lmliam.microsmith.lower.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact

internal object MsBuildProjectXmlRenderer {
    fun render(artifact: MsBuildProjectArtifact): String = buildString {
        appendLine("<Project>")
        if (artifact.properties.isNotEmpty()) {
            appendLine("  <PropertyGroup>")
            artifact.properties.forEach { (name, value) ->
                appendLine("    <$name>${xmlEscape(value)}</$name>")
            }
            appendLine("  </PropertyGroup>")
        }
        if (artifact.items.isNotEmpty()) {
            appendLine("  <ItemGroup>")
            artifact.items.forEach { item ->
                append("    <${item.type} Include=\"")
                append(xmlEscape(item.include))
                append("\"")
                item.metadata.toSortedMap().forEach { (key, value) ->
                    append(" $key=\"")
                    append(xmlEscape(value))
                    append("\"")
                }
                appendLine(" />")
            }
            appendLine("  </ItemGroup>")
        }
        appendLine("</Project>")
    }

    private fun xmlEscape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '\"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(character)
            }
        }
    }
}
