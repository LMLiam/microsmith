package io.github.lmliam.microsmith.build.cli

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

internal object CliSourceFiles {
    private val PACKAGE_PATTERN: Pattern = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)\\s*$")
    private val TOP_LEVEL_FUNCTION_PATTERN: Pattern =
        Pattern.compile(
            "(?m)^\\s*(?:@[A-Za-z0-9_.]+(?:\\([^\\n]*?\\))?\\s*)*" +
                "(?:(?:public|private|protected|internal|protected\\s+internal)\\s+)?" +
                "(?:(?:suspend|inline|tailrec|operator|infix|external|override)\\s+)*" +
                "fun\\s+([A-Za-z0-9_]+)\\b",
        )

    fun mainFunctionSourceFile(project: Project): File {
        return mainFunctionSourceFile(project.layout.projectDirectory.asFile.resolve("src/main/kotlin"))
    }

    fun mainFunctionSourceFile(sourceRoot: File): File {
        return sourceFileByTopLevelFunction(sourceRoot, "main")
    }

    fun mainClassNameFromSourceFile(sourceFile: File): String {
        return "${sourceFile.nameWithoutExtension}Kt"
    }

    fun packageNameFromSourceFile(sourceFile: File): String {
        val packageMatcher = PACKAGE_PATTERN.matcher(sourceFile.readText(StandardCharsets.UTF_8))
        if (!packageMatcher.find()) {
            throw GradleException("Source file '${sourceFile.path}' did not declare a package.")
        }

        return packageMatcher.group(1)
    }

    fun applicationMainClass(project: Project): String {
        return applicationMainClassFromSourceFile(mainFunctionSourceFile(project))
    }

    fun applicationMainClassFromSourceFile(sourceFile: File): String {
        return "${packageNameFromSourceFile(sourceFile)}.${mainClassNameFromSourceFile(sourceFile)}"
    }

    fun applicationMainClassFromSourceRoot(sourceRoot: File): String {
        return applicationMainClassFromSourceFile(mainFunctionSourceFile(sourceRoot))
    }

    fun sourceFileByTopLevelFunction(sourceRoot: File, functionName: String): File {
        val sourceFiles = sourceRoot
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .filter { file -> topLevelFunctionNameOrNull(file) == functionName }
            .toList()

        if (sourceFiles.size != 1) {
            throw GradleException(
                "Expected exactly one source file declaring top-level function '$functionName' but found ${sourceFiles.map { it.path }}",
            )
        }

        return sourceFiles.single()
    }

    private fun topLevelFunctionNameOrNull(sourceFile: File): String? {
        val declarationMatcher = TOP_LEVEL_FUNCTION_PATTERN.matcher(sourceFile.readText(StandardCharsets.UTF_8))
        if (!declarationMatcher.find()) {
            return null
        }

        return declarationMatcher.group(1)
    }
}
