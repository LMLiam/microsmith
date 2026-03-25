package io.github.lmliam.microsmith.gen.services.dotnet.msbuild

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

@ServiceProvider(ArtifactRenderer::class)
class MsBuildProjectArtifactRenderer : ArtifactRenderer<MsBuildProjectArtifact> {
    override val artifactType: KClass<MsBuildProjectArtifact> = MsBuildProjectArtifact::class

    override fun render(artifact: MsBuildProjectArtifact): GeneratedFile {
        val contents = buildString {
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

        return GeneratedFile(
            relativePath = artifact.id.relativePath,
            contents = contents.toByteArray(StandardCharsets.UTF_8),
            outputRoot = artifact.id.outputRoot,
        )
    }

    private fun xmlEscape(value: String): String {
        return buildString(value.length) {
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
}
