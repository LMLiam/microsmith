package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildAttributeName
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildPropertyName

internal object MsBuildProjectXmlRenderer {
    fun render(artifact: MsBuildProjectArtifact): String = buildString {
        appendLine("<Project>")
        if (artifact.properties.isNotEmpty()) {
            appendLine("  <PropertyGroup>")
            artifact.properties.toSortedMap(compareBy(MsBuildPropertyName::value)).forEach { (name, value) ->
                appendLine("    <${name.value}>${xmlEscape(value)}</${name.value}>")
            }
            appendLine("  </PropertyGroup>")
        }
        if (artifact.items.isNotEmpty()) {
            appendLine("  <ItemGroup>")
            artifact.items.forEach { item ->
                append("    <${item.itemName} Include=\"")
                append(xmlEscape(item.include))
                append("\"")
                item.attributes.toSortedMap(compareBy(MsBuildAttributeName::value)).forEach { (key, value) ->
                    append(" ${key.value}=\"")
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
