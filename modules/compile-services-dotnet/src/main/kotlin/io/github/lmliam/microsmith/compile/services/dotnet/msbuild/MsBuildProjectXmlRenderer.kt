package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import java.io.StringWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

internal object MsBuildProjectXmlRenderer {
    fun render(artifact: MsBuildProjectArtifact): String {
        val output = StringWriter()
        val writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output)
        try {
            writer.writeStartElement(PROJECT_ELEMENT_NAME)
            renderProperties(writer, artifact)
            renderItems(writer, artifact)
            writer.writeCharacters(NEW_LINE)
            writer.writeEndElement()
            writer.flush()
        } finally {
            writer.close()
        }
        return output.toString()
    }

    private fun renderProperties(writer: XMLStreamWriter, artifact: MsBuildProjectArtifact) {
        if (artifact.properties.isNotEmpty()) {
            writer.writeIndentedStartElement(PROPERTY_GROUP_ELEMENT_NAME, level = 1)
            artifact.properties.toSortedMap().forEach { (name, value) ->
                writer.writeIndentedStartElement(name, level = 2)
                writer.writeCharacters(value)
                writer.writeEndElement()
            }
            writer.writeIndentedEndElement(level = 1)
        }
    }

    private fun renderItems(writer: XMLStreamWriter, artifact: MsBuildProjectArtifact) {
        if (artifact.items.isNotEmpty()) {
            writer.writeIndentedStartElement(ITEM_GROUP_ELEMENT_NAME, level = 1)
            artifact.items.forEach { item ->
                writer.writeIndent(level = 2)
                writer.writeEmptyElement(item.itemName)
                writer.writeAttribute(MsBuildNames.requireAttributeName(INCLUDE_ATTRIBUTE_NAME), item.include)
                item.attributes.toSortedMap().forEach { (name, value) ->
                    writer.writeAttribute(name, value)
                }
            }
            writer.writeIndentedEndElement(level = 1)
        }
    }

    private fun XMLStreamWriter.writeIndentedStartElement(name: String, level: Int) {
        writeIndent(level)
        writeStartElement(name)
    }

    private fun XMLStreamWriter.writeIndentedEndElement(level: Int) {
        writeIndent(level)
        writeEndElement()
    }

    private fun XMLStreamWriter.writeIndent(level: Int) {
        writeCharacters(NEW_LINE)
        repeat(level) {
            writeCharacters(INDENT)
        }
    }

    private const val PROJECT_ELEMENT_NAME = "Project"
    private const val PROPERTY_GROUP_ELEMENT_NAME = "PropertyGroup"
    private const val ITEM_GROUP_ELEMENT_NAME = "ItemGroup"
    private const val INCLUDE_ATTRIBUTE_NAME = "Include"
    private const val NEW_LINE = "\n"
    private const val INDENT = "  "
}
