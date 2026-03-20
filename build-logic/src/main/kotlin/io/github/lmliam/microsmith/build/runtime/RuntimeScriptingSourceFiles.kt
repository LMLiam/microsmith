package io.github.lmliam.microsmith.build.runtime

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

internal object RuntimeScriptingSourceFiles {
    private val PACKAGE_PATTERN: Pattern = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)\\s*$")

    fun annotatedKotlinScriptSourceFile(project: Project, fileExtension: String): File {
        val sourceFiles =
            sourceRoot(project)
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

    fun sourceFileBySimpleName(project: Project, simpleName: String): File {
        val sourceFiles =
            sourceRoot(project)
                .walkTopDown()
                .filter { file -> file.isFile && file.name == "$simpleName.kt" }
                .toList()

        if (sourceFiles.size != 1) {
            throw GradleException(
                "Expected exactly one source file named '$simpleName.kt' but found ${sourceFiles.map { it.path }}",
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

    fun fqcnFromSourceFile(sourceFile: File): String {
        val sourceFileName = sourceFile.name
        val extensionIndex = sourceFileName.lastIndexOf('.')
        val simpleName = if (extensionIndex >= 0) sourceFileName.substring(0, extensionIndex) else sourceFileName
        return "${packageNameFromSourceFile(sourceFile)}.$simpleName"
    }

    private fun sourceRoot(project: Project): File =
        project.layout.projectDirectory.asFile.resolve("src/main/kotlin")
}
