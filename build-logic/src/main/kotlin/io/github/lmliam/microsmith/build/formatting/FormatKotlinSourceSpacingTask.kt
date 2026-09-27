package io.github.lmliam.microsmith.build.formatting

import io.github.lmliam.microsmith.build.quality.TopLevelKotlinLineScanner
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@DisableCachingByDefault(because = "This formatter edits source files in place.")
abstract class FormatKotlinSourceSpacingTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    init {
        group = "formatting"
        description = "Adds one blank line after Kotlin package and import declarations."
    }

    @TaskAction
    fun format() {
        sourceFiles.files
            .asSequence()
            .filter { file -> file.isFile }
            .sortedBy { file -> file.path }
            .forEach { file ->
                val source = Files.readString(file.toPath(), StandardCharsets.UTF_8)
                val formatted = KotlinSourceSpacingFormatter.format(source)
                if (formatted != source) {
                    Files.writeString(file.toPath(), formatted, StandardCharsets.UTF_8)
                }
            }
    }
}

internal object KotlinSourceSpacingFormatter {
    fun format(source: String): String {
        val lines = source.toSourceLines()
        if (lines.isEmpty()) return source

        val packageLineIndex = lines.topLevelDeclarationIndex(PACKAGE_KEYWORD)
        if (packageLineIndex != null) {
            lines.ensureOneBlankLineAfter(packageLineIndex)
        }

        val importLineIndex = lines.topLevelDeclarationIndex(IMPORT_KEYWORD, last = true)
        if (importLineIndex != null) {
            lines.ensureOneBlankLineAfter(importLineIndex)
        }

        return lines.joinToString(separator = "") { line -> line.content + line.lineEnding }
    }

    private fun MutableList<SourceLine>.topLevelDeclarationIndex(
        keyword: String,
        last: Boolean = false,
    ): Int? {
        val declarations = TopLevelKotlinLineScanner.scanWithIndexes(map(SourceLine::content))
            .filter { (_, line) -> line.trimStart().startsWithKeyword(keyword) }
        return if (last) declarations.lastOrNull()?.index else declarations.firstOrNull()?.index
    }

    private fun String.startsWithKeyword(keyword: String): Boolean {
        val declaration = trimStart()
        return declaration.startsWith("$keyword ") || declaration.startsWith("$keyword\t")
    }

    private fun MutableList<SourceLine>.ensureOneBlankLineAfter(lineIndex: Int) {
        var nextContentIndex = lineIndex + 1
        while (nextContentIndex < size && this[nextContentIndex].content.isBlank()) {
            nextContentIndex += 1
        }
        if (nextContentIndex == size) return

        val blankLineCount = nextContentIndex - lineIndex - 1
        if (blankLineCount == 1 && this[lineIndex + 1].content.isEmpty()) return

        repeat(blankLineCount) {
            removeAt(lineIndex + 1)
        }
        val lineEnding = this[lineIndex].lineEnding.takeIf { ending -> ending.isNotEmpty() }
            ?: getOrNull(lineIndex + 1)?.lineEnding?.takeIf { ending -> ending.isNotEmpty() }
            ?: "\n"
        add(lineIndex + 1, SourceLine(content = "", lineEnding = lineEnding))
    }

    private fun String.toSourceLines(): MutableList<SourceLine> {
        val lines = mutableListOf<SourceLine>()
        var lineStart = 0
        var index = 0
        while (index < length) {
            if (this[index] == '\n' || this[index] == '\r') {
                val lineEndingLength = if (this[index] == '\r' && getOrNull(index + 1) == '\n') 2 else 1
                lines += SourceLine(
                    content = substring(lineStart, index),
                    lineEnding = substring(index, index + lineEndingLength),
                )
                index += lineEndingLength
                lineStart = index
            } else {
                index += 1
            }
        }
        if (lineStart < length) {
            lines += SourceLine(content = substring(lineStart), lineEnding = "")
        }
        return lines
    }

    private const val PACKAGE_KEYWORD = "package"
    private const val IMPORT_KEYWORD = "import"
}

private data class SourceLine(
    val content: String,
    val lineEnding: String,
)
