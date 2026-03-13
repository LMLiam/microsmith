package me.liam.microsmith.gradle

import java.nio.file.Path

internal object MicrosmithGradleFunctionalTestRuntimeClasspath {
    fun buildScriptDependencyBlock(): String {
        val files =
            entries().joinToString(separator = ",\n        ") { path ->
                "\"${path.toString().replace("\\", "\\\\")}\""
            }
        return """
            dependencies {
                add("${MicrosmithGradleConfigurations.RUNTIME}", files(
                    $files
                ))
            }
        """.trimIndent()
    }

    private fun entries(): List<Path> {
        val classpath =
            System.getProperty("java.class.path")
                ?.takeIf(String::isNotBlank)
                ?: error("Expected a non-empty java.class.path for Gradle functional tests.")
        return classpath.split(FilePathSeparator.value)
            .filter(String::isNotBlank)
            .map(Path::of)
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .distinct()
    }
}
