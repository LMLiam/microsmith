package io.github.lmliam.microsmith.build.runtime

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

internal object RuntimeScriptingSourceFiles {
    private val PACKAGE_PATTERN: Pattern = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)\\s*$")
    private val TOP_LEVEL_DECLARATION_PATTERN: Pattern =
        Pattern.compile(
            "(?m)^\\s*(?:@[A-Za-z0-9_.]+(?:\\([^\\n]*?\\))?\\s*)*" +
                "(?:(?:public|private|protected|internal|protected\\s+internal)\\s+)?" +
                "(?:(?:abstract|final|sealed|data|inner|enum|annotation)\\s+)*" +
                "(?:class|object)\\s+([A-Za-z0-9_]+)\\b",
        )
    private val COMPILATION_CONFIGURATION_PATTERN: Pattern =
        Pattern.compile("(?m)compilationConfiguration\\s*=\\s*([A-Za-z0-9_.]+)::class")
    private val IMPLICIT_RECEIVER_PATTERN: Pattern =
        Pattern.compile("(?m)implicitReceivers\\s*\\(\\s*([A-Za-z0-9_.]+)::class")

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
        val packageMatcher = PACKAGE_PATTERN.matcher(sourceFile.readText(StandardCharsets.UTF_8))
        if (!packageMatcher.find()) {
            throw GradleException("Source file '${sourceFile.path}' did not declare a package.")
        }

        return packageMatcher.group(1)
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
                COMPILATION_CONFIGURATION_PATTERN,
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
                IMPLICIT_RECEIVER_PATTERN,
                "script context",
            ),
        )
    }

    private fun topLevelDeclarationNameOrNull(sourceFile: File): String? {
        val declarationMatcher = TOP_LEVEL_DECLARATION_PATTERN.matcher(sourceFile.readText(StandardCharsets.UTF_8))
        if (!declarationMatcher.find()) {
            return null
        }

        return declarationMatcher.group(1)
    }

    private fun referencedTypeSimpleNameFromSourceFile(
        sourceFile: File,
        pattern: Pattern,
        description: String,
    ): String {
        val sourceText = sourceFile.readText(StandardCharsets.UTF_8)
        val matcher = pattern.matcher(sourceText)
        if (!matcher.find()) {
            throw GradleException("Source file '${sourceFile.path}' did not reference a $description type.")
        }

        return matcher.group(1).substringAfterLast('.')
    }

    private fun sourceRoot(project: Project): File = project.layout.projectDirectory.asFile.resolve("src/main/kotlin")
}
