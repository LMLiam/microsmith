package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.GeneratedFile
import java.nio.charset.StandardCharsets
import java.nio.file.Path

internal object GeneratedOriginsManifestBuilder {
    private const val FIRST_PRINTABLE_CHARACTER_CODE = 0x20
    private val manifestRelativePath = Path.of(".microsmith", "origins.json")

    fun appendTo(outputs: List<GeneratedFile>): List<GeneratedFile> {
        val manifests = outputs
            .groupBy(GeneratedFile::outputRoot)
            .mapNotNull { (outputRoot, files) ->
                val tracedFiles = files
                    .filter { it.relativePath != manifestRelativePath }
                    .map { file ->
                        TracedFile(
                            relativePath = file.relativePath.toString().replace('\\', '/'),
                            origins = file.origins.toList().sorted(),
                        )
                    }.sortedBy(TracedFile::relativePath)
                if (tracedFiles.isEmpty()) {
                    return@mapNotNull null
                }
                GeneratedFile(
                    relativePath = manifestRelativePath,
                    contents = renderManifest(tracedFiles).toByteArray(StandardCharsets.UTF_8),
                    outputRoot = outputRoot,
                    origins = tracedFiles.flatMapTo(sortedSetOf()) { it.origins },
                )
            }
        return outputs + manifests
    }

    private fun renderManifest(files: List<TracedFile>): String = buildString {
        appendLine("{")
        appendLine("""  "generatedBy": "Microsmith",""")
        appendLine("""  "files": [""")
        files.forEachIndexed { index, file ->
            appendLine("    {")
            appendLine("""      "path": "${escapeJson(file.relativePath)}",""")
            appendLine("""      "origins": [""")
            file.origins.forEachIndexed { originIndex, origin ->
                val suffix = if (originIndex == file.origins.lastIndex) "" else ","
                appendLine("""        "${escapeJson(origin)}"$suffix""")
            }
            val fileSuffix = if (index == files.lastIndex) "" else ","
            appendLine("      ]")
            appendLine("    }$fileSuffix")
        }
        appendLine("  ]")
        append('}')
    }

    private fun escapeJson(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < FIRST_PRINTABLE_CHARACTER_CODE) {
                        append("\\u%04x".format(char.code))
                    } else {
                        append(char)
                    }
                }
            }
        }
    }

    private data class TracedFile(val relativePath: String, val origins: List<String>)
}
