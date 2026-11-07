package io.github.lmliam.microsmith.build.runtime

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyPublishedPomScopes : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val pomFile: RegularFileProperty

    @get:Input
    abstract val expectedScopes: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val artifactDescription: Property<String>

    init {
        group = "verification"
        description = "Verifies dependency scopes in the published POM file."
        artifactDescription.convention("POM")
    }

    @TaskAction
    fun verify() {
        val pom = pomFile.asFile.get()
        val description = artifactDescription.get()
        if (!pom.isFile) {
            throw GradleException("$description '${pom.path}' was not created.")
        }

        val expectations = expectedScopes.get().map { parseExpectation(it, description) }
        val declaredDependencies = pom.readDeclaredDependencies()

        val missingOrMismatch =
            expectations.filterNot { expectation ->
                declaredDependencies.any { actual ->
                    actual.groupId == expectation.groupId &&
                        actual.artifactId == expectation.artifactId &&
                        actual.scope == expectation.scope
                }
            }

        if (missingOrMismatch.isNotEmpty()) {
            val formattedFailures =
                missingOrMismatch.joinToString(", ") {
                    "${it.groupId}:${it.artifactId} (expected scope '${it.scope}')"
                }
            throw GradleException(
                "$description '${pom.name}' had unexpected or missing dependency scopes: $formattedFailures",
            )
        }
    }

    private fun parseExpectation(
        declaration: String,
        description: String,
    ): DependencyExpectation {
        val parts = declaration.split('=', limit = 2)
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw GradleException(
                "Invalid expected dependency '$declaration' for $description. " +
                    "Expected '<groupId>:<artifactId>=<scope>'.",
            )
        }

        val coords = parts[0].trim().split(':', limit = 2)
        if (coords.size != 2 || coords[0].isBlank() || coords[1].isBlank()) {
            throw GradleException(
                "Invalid expected dependency '$declaration' for $description. " +
                    "Expected '<groupId>:<artifactId>=<scope>'.",
            )
        }

        return DependencyExpectation(
            groupId = coords[0].trim(),
            artifactId = coords[1].trim(),
            scope = parts[1].trim(),
        )
    }

    private data class DependencyExpectation(
        val groupId: String,
        val artifactId: String,
        val scope: String,
    )
}

private data class DeclaredDependency(
    val groupId: String,
    val artifactId: String,
    val scope: String,
)

private val DEPENDENCY_BLOCK_REGEX = Regex("(?s)<dependency>(.*?)</dependency>")
private val GROUP_ID_REGEX = Regex("<groupId>\\s*(.*?)\\s*</groupId>")
private val ARTIFACT_ID_REGEX = Regex("<artifactId>\\s*(.*?)\\s*</artifactId>")
private val SCOPE_REGEX = Regex("<scope>\\s*(.*?)\\s*</scope>")

private fun File.readDeclaredDependencies(): List<DeclaredDependency> {
    val pomContent = readText()
    return DEPENDENCY_BLOCK_REGEX
        .findAll(pomContent)
        .mapNotNull { match ->
            val block = match.groupValues[1]
            val groupId = GROUP_ID_REGEX.find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            val artifactId = ARTIFACT_ID_REGEX.find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            val scope = SCOPE_REGEX.find(block)?.groupValues?.get(1) ?: "compile"

            DeclaredDependency(groupId, artifactId, scope)
        }.toList()
}
