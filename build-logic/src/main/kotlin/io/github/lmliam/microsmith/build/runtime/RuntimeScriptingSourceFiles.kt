package io.github.lmliam.microsmith.build.runtime

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.io.File
import java.nio.charset.StandardCharsets

internal object RuntimeScriptingSourceFiles {
    private val DECLARATION_MODIFIER_PREFIXES = listOf(
        "protected internal ",
        "public ",
        "private ",
        "protected ",
        "internal ",
        "abstract ",
        "final ",
        "sealed ",
        "data ",
        "inner ",
        "enum ",
        "annotation ",
    )
    private const val PACKAGE_DECLARATION_PREFIX = "package "
    private const val CLASS_DECLARATION_PREFIX = "class "
    private const val OBJECT_DECLARATION_PREFIX = "object "
    private const val COMPILATION_CONFIGURATION_MARKER = "compilationConfiguration ="
    private const val IMPLICIT_RECEIVER_MARKER = "implicitReceivers("

    fun annotatedKotlinScriptSourceFile(project: Project, fileExtension: String): File {
        return annotatedKotlinScriptSourceFile(sourceRoot(project), fileExtension)
    }

    fun annotatedKotlinScriptSourceFile(sourceRoot: File, fileExtension: String): File {
        val sourceFiles =
            sourceRoot
                .walkTopDown()
                .filter { file -> file.isFile && file.extension == "kt" }
                .filter { file ->
                    val sourceText = file.readText(StandardCharsets.UTF_8)
                    sourceText.contains("@KotlinScript(") && sourceText.contains("fileExtension = \"$fileExtension\"")
                }
                .toList()

        if (sourceFiles.size != 1) {
            throw GradleException(
                "Expected exactly one Kotlin script template source for extension '$fileExtension' but found ${sourceFiles.map { it.path }}",
            )
        }

        return sourceFiles.single()
    }

    fun sourceFileByTopLevelDeclaration(project: Project, declarationName: String): File {
        return sourceFileByTopLevelDeclaration(sourceRoot(project), declarationName)
    }

    fun sourceFileByTopLevelDeclaration(sourceRoot: File, declarationName: String): File {
        val sourceFiles = sourceRoot
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .filter { file -> topLevelDeclarationNameOrNull(file) == declarationName }
            .toList()

        if (sourceFiles.size != 1) {
            throw GradleException(
                "Expected exactly one source file declaring '$declarationName' but found ${sourceFiles.map { it.path }}",
            )
        }

        return sourceFiles.single()
    }

    fun packageNameFromSourceFile(sourceFile: File): String {
        return firstMatchingLine(sourceFile) { line ->
            val trimmedLine = line.trim()
            if (!trimmedLine.startsWith(PACKAGE_DECLARATION_PREFIX)) {
                return@firstMatchingLine null
            }

            trimmedLine
                .removePrefix(PACKAGE_DECLARATION_PREFIX)
                .trim()
                .takeIf { it.isNotBlank() }
        } ?: throw GradleException("Source file '${sourceFile.path}' did not declare a package.")
    }

    fun topLevelDeclarationNameFromSourceFile(sourceFile: File): String {
        return topLevelDeclarationNameOrNull(sourceFile)
            ?: throw GradleException("Source file '${sourceFile.path}' did not declare a top level class or object.")
    }

    fun fqcnFromSourceFile(sourceFile: File): String {
        return "${packageNameFromSourceFile(sourceFile)}.${topLevelDeclarationNameFromSourceFile(sourceFile)}"
    }

    fun compilationConfigurationSourceFileFromScriptTemplateSourceFile(
        project: Project,
        sourceFile: File,
    ): File {
        return compilationConfigurationSourceFileFromScriptTemplateSourceFile(sourceRoot(project), sourceFile)
    }

    fun compilationConfigurationSourceFileFromScriptTemplateSourceFile(
        sourceRoot: File,
        sourceFile: File,
    ): File {
        return sourceFileByTopLevelDeclaration(
            sourceRoot,
            referencedTypeSimpleNameFromSourceFile(
                sourceFile,
                COMPILATION_CONFIGURATION_MARKER,
                "compilation configuration",
            ),
        )
    }

    fun contextSourceFileFromCompilationConfigurationSourceFile(
        project: Project,
        sourceFile: File,
    ): File {
        return contextSourceFileFromCompilationConfigurationSourceFile(sourceRoot(project), sourceFile)
    }

    fun contextSourceFileFromCompilationConfigurationSourceFile(
        sourceRoot: File,
        sourceFile: File,
    ): File {
        return sourceFileByTopLevelDeclaration(
            sourceRoot,
            referencedTypeSimpleNameFromSourceFile(
                sourceFile,
                IMPLICIT_RECEIVER_MARKER,
                "script context",
            ),
        )
    }

    private fun topLevelDeclarationNameOrNull(sourceFile: File): String? {
        return firstMatchingLine(sourceFile) { line ->
            val trimmedLine = line.trimStart()
            if (trimmedLine.isBlank() || trimmedLine.startsWith("//") || trimmedLine.startsWith("/*") || trimmedLine.startsWith("*")) {
                return@firstMatchingLine null
            }
            if (trimmedLine.startsWith("@")) {
                return@firstMatchingLine null
            }

            val declarationLine = removeLeadingDeclarationModifiers(trimmedLine)
            when {
                declarationLine.startsWith(CLASS_DECLARATION_PREFIX) ->
                    extractDeclarationName(declarationLine, CLASS_DECLARATION_PREFIX)
                declarationLine.startsWith(OBJECT_DECLARATION_PREFIX) ->
                    extractDeclarationName(declarationLine, OBJECT_DECLARATION_PREFIX)
                else -> null
            }
        }
    }

    private fun referencedTypeSimpleNameFromSourceFile(
        sourceFile: File,
        marker: String,
        description: String,
    ): String {
        val sourceText = sourceFile.readText(StandardCharsets.UTF_8)
        val markerIndex = sourceText.indexOf(marker)
        if (markerIndex < 0) {
            throw GradleException("Source file '${sourceFile.path}' did not reference a $description type.")
        }

        val classIndex = sourceText.indexOf("::class", startIndex = markerIndex + marker.length)
        if (classIndex < 0) {
            throw GradleException("Source file '${sourceFile.path}' did not reference a $description type.")
        }

        val referencedType = sourceText
            .substring(markerIndex + marker.length, classIndex)
            .trim()
            .trimEnd(',', ')')
        val simpleName = referencedType.substringAfterLast('.').takeWhile { char -> char.isLetterOrDigit() || char == '_' }
        if (simpleName.isBlank()) {
            throw GradleException("Source file '${sourceFile.path}' did not reference a valid $description type.")
        }

        return simpleName
    }

    private fun firstMatchingLine(sourceFile: File, matcher: (String) -> String?): String? {
        val sourceText = sourceFile.readText(StandardCharsets.UTF_8)
        return sourceText.lineSequence().mapNotNull(matcher).firstOrNull()
    }

    private fun removeLeadingDeclarationModifiers(line: String): String {
        var candidate = line
        while (true) {
            val matchedPrefix = DECLARATION_MODIFIER_PREFIXES.firstOrNull { prefix ->
                candidate.startsWith(prefix)
            } ?: break
            candidate = candidate.removePrefix(matchedPrefix)
        }

        return candidate
    }

    private fun extractDeclarationName(line: String, declarationPrefix: String): String? {
        val declarationRemainder = line.removePrefix(declarationPrefix).trimStart()
        if (declarationRemainder.isBlank()) {
            return null
        }

        val declarationName = declarationRemainder.takeWhile { char ->
            char.isLetterOrDigit() || char == '_'
        }
        return declarationName.takeIf { it.isNotBlank() }
    }

    private fun sourceRoot(project: Project): File = project.layout.projectDirectory.asFile.resolve("src/main/kotlin")
}
