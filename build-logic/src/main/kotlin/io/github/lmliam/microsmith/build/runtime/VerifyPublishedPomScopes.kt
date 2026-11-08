package io.github.lmliam.microsmith.build.runtime

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when a published POM gives a dependency an unexpected scope.
 *
 * Each expected dependency is declared as `<groupId>:<artifactId>=<scope>`.
 */
abstract class VerifyPublishedPomScopes : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val pomFile: RegularFileProperty

    @get:Input
    abstract val expectedScopes: ListProperty<String>

    @get:Input
    abstract val artifactDescription: Property<String>

    @TaskAction
    fun verify() {
        val pom = pomFile.get().asFile
        val description = artifactDescription.get()
        if (!pom.isFile) {
            throw GradleException("$description '${pom.path}' was not created.")
        }

        val pomContents = pom.readText(StandardCharsets.UTF_8)
        val unexpectedScopes =
            expectedScopes.get().mapNotNull { declaration ->
                val separatorIndex = declaration.indexOf('=')
                if (separatorIndex < 0) {
                    throw GradleException(
                        "Invalid expected dependency '$declaration' for $description. " +
                            "Expected '<groupId>:<artifactId>=<scope>'.",
                    )
                }
                val coordinates = declaration.substring(0, separatorIndex)
                val expectedScope = declaration.substring(separatorIndex + 1)
                val components = coordinates.split(":")
                if (components.size != 2) {
                    throw GradleException(
                        "Invalid expected dependency '$coordinates' for $description. " +
                            "Expected '<groupId>:<artifactId>=<scope>'.",
                    )
                }
                if (declaresScope(pomContents, components[0], components[1], expectedScope)) {
                    null
                } else {
                    "$coordinates expected $expectedScope scope"
                }
            }

        if (unexpectedScopes.isNotEmpty()) {
            throw GradleException(
                "$description '${pom.name}' had unexpected dependency scopes: ${unexpectedScopes.joinToString(", ")}",
            )
        }
    }
}

private fun declaresScope(
    pomContents: String,
    groupId: String,
    artifactId: String,
    expectedScope: String,
): Boolean {
    val dependencyPattern =
        Pattern.compile(
            "(?s)<dependency>\\s*<groupId>${Pattern.quote(groupId)}</groupId>\\s*" +
                "<artifactId>${Pattern.quote(artifactId)}</artifactId>\\s*" +
                "<version>[^<]+</version>\\s*" +
                "<scope>${Pattern.quote(expectedScope)}</scope>\\s*</dependency>",
        )
    return dependencyPattern.matcher(pomContents).find()
}
