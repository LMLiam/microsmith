package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildAttributeName
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildPropertyName

internal object MsBuildProjectXmlRenderer {
    fun render(artifact: MsBuildProjectArtifact): String = buildString {
        appendLine(openElement(ProjectElementName))
        if (artifact.properties.isNotEmpty()) {
            appendLine(indentedOpenElement(PropertyGroupElementName))
            artifact.properties.toSortedMap(compareBy(MsBuildPropertyName::value)).forEach { (name, value) ->
                appendLine("    <${name.value}>${xmlEscape(value)}</${name.value}>")
            }
            appendLine(indentedCloseElement(PropertyGroupElementName))
        }
        if (artifact.items.isNotEmpty()) {
            appendLine(indentedOpenElement(ItemGroupElementName))
            artifact.items.forEach { item ->
                append("    <${item.itemName.value} $IncludeAttributeName=\"")
                append(xmlEscape(item.include))
                append("\"")
                item.attributes.toSortedMap(compareBy(MsBuildAttributeName::value)).forEach { (key, value) ->
                    append(" ${key.value}=\"")
                    append(xmlEscape(value))
                    append("\"")
                }
                appendLine(" />")
            }
            appendLine(indentedCloseElement(ItemGroupElementName))
        }
        appendLine(closeElement(ProjectElementName))
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

    private fun openElement(name: String): String = "<$name>"

    private fun closeElement(name: String): String = "</$name>"

    private fun indentedOpenElement(name: String): String = "  ${openElement(name)}"

    private fun indentedCloseElement(name: String): String = "  ${closeElement(name)}"

    private const val ProjectElementName = "Project"
    private const val PropertyGroupElementName = "PropertyGroup"
    private const val ItemGroupElementName = "ItemGroup"
    private const val IncludeAttributeName = "Include"
}
