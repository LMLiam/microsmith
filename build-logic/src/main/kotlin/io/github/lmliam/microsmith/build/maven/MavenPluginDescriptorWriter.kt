package io.github.lmliam.microsmith.build.maven

import org.gradle.api.artifacts.Configuration

import java.io.File
import java.nio.charset.StandardCharsets

internal data class RuntimeArtifact(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val type: String,
    val classifier: String?,
)

internal object MavenPluginDescriptorWriter {
    fun write(
        outputFile: File,
        pluginGroupId: String,
        pluginArtifactId: String,
        pluginVersion: String,
        goalPrefixValue: String,
        generateGoalValue: String,
        runtimeArtifacts: List<RuntimeArtifact>,
    ) {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                appendContainer(0, "plugin") {
                    appendTextElement(1, "name", MavenPluginBuildNames.PLUGIN_NAME)
                    appendTextElement(1, "description", MavenPluginBuildNames.PLUGIN_DESCRIPTION)
                    appendTextElement(1, "groupId", pluginGroupId)
                    appendTextElement(1, "artifactId", pluginArtifactId)
                    appendTextElement(1, "version", pluginVersion)
                    appendTextElement(1, "goalPrefix", goalPrefixValue)
                    appendTextElement(1, "isolatedRealm", "false")
                    appendTextElement(1, "inheritedByDefault", "true")
                    appendContainer(1, "mojos") {
                        appendContainer(2, "mojo") {
                            appendTextElement(3, "goal", generateGoalValue)
                            appendTextElement(3, "description", "Generates artifacts from a .microsmith.kts script.")
                            appendTextElement(3, "implementation", MavenPluginBuildNames.IMPLEMENTATION_CLASS)
                            appendTextElement(3, "language", "java")
                            appendTextElement(3, "phase", "generate-resources")
                            appendTextElement(3, "instantiationStrategy", "per-lookup")
                            appendTextElement(3, "executionStrategy", "once-per-session")
                            appendTextElement(3, "requiresProject", "true")
                            appendTextElement(3, "threadSafe", "true")
                            appendContainer(3, "parameters") {
                                appendParameter(
                                    4,
                                    "projectBaseDirectory",
                                    "java.io.File",
                                    required = false,
                                    editable = false,
                                    description = "Repository base directory used to resolve relative Microsmith paths.",
                                )
                                appendParameter(
                                    4,
                                    "scriptFile",
                                    "java.io.File",
                                    required = false,
                                    editable = true,
                                    description = "Path to the .microsmith.kts script to execute.",
                                )
                                appendParameter(
                                    4,
                                    "outputDirectory",
                                    "java.io.File",
                                    required = false,
                                    editable = true,
                                    description = "Directory where generated outputs are written.",
                                )
                                appendParameter(
                                    4,
                                    "cacheDirectory",
                                    "java.io.File",
                                    required = false,
                                    editable = true,
                                    description = "Directory used for Microsmith script compilation cache entries.",
                                )
                                appendParameter(
                                    4,
                                    "variables",
                                    "java.util.Properties",
                                    required = false,
                                    editable = true,
                                    description = "Variables exposed to the script via requireVar and hasVar.",
                                )
                                appendParameter(
                                    4,
                                    "flags",
                                    "java.util.List",
                                    required = false,
                                    editable = true,
                                    description = "Flags exposed to the script via hasFlag.",
                                )
                            }
                            appendContainer(3, "configuration") {
                                appendConfigurationElement(
                                    4,
                                    "projectBaseDirectory",
                                    implementation = "java.io.File",
                                    defaultValue = "\${project.basedir}",
                                    value = "\${project.basedir}",
                                )
                                appendConfigurationElement(
                                    4,
                                    "scriptFile",
                                    implementation = "java.io.File",
                                    defaultValue = "\${project.basedir}/build.microsmith.kts",
                                    value = "\${microsmith.scriptFile}",
                                )
                                appendConfigurationElement(
                                    4,
                                    "outputDirectory",
                                    implementation = "java.io.File",
                                    defaultValue = "\${project.build.directory}/generated/microsmith",
                                    value = "\${microsmith.outputDirectory}",
                                )
                                appendConfigurationElement(
                                    4,
                                    "cacheDirectory",
                                    implementation = "java.io.File",
                                    defaultValue = "\${project.build.directory}/tmp/microsmith/cache",
                                    value = "\${microsmith.cacheDirectory}",
                                )
                                appendConfigurationElement(
                                    4,
                                    "variables",
                                    implementation = "java.util.Properties",
                                    value = "\${microsmith.variables}",
                                )
                                appendConfigurationElement(
                                    4,
                                    "flags",
                                    implementation = "java.util.List",
                                    value = "\${microsmith.flags}",
                                )
                            }
                        }
                    }
                    appendContainer(1, "dependencies") {
                        runtimeArtifacts.forEach { runtimeArtifact ->
                            appendContainer(2, "dependency") {
                                appendTextElement(3, "groupId", runtimeArtifact.groupId)
                                appendTextElement(3, "artifactId", runtimeArtifact.artifactId)
                                appendTextElement(3, "type", runtimeArtifact.type)
                                appendTextElement(3, "version", runtimeArtifact.version)
                                runtimeArtifact.classifier?.let { classifier ->
                                    appendTextElement(3, "classifier", classifier)
                                }
                            }
                        }
                    }
                }
            },
            StandardCharsets.UTF_8,
        )
    }

    fun resolveRuntimeArtifacts(runtimeClasspath: Configuration): List<RuntimeArtifact> =
        runtimeClasspath.resolvedConfiguration.resolvedArtifacts
            .map { artifact ->
                RuntimeArtifact(
                    groupId = artifact.moduleVersion.id.group,
                    artifactId = artifact.name,
                    version = artifact.moduleVersion.id.version,
                    type = artifact.extension ?: "jar",
                    classifier = artifact.classifier,
                )
            }.sortedWith(
                compareBy<RuntimeArtifact> { it.groupId }
                    .thenBy { it.artifactId }
                    .thenBy { it.version }
                    .thenBy { it.classifier ?: "" },
            )
}

private fun StringBuilder.appendContainer(level: Int, name: String, block: StringBuilder.() -> Unit) {
    appendIndent(level)
    append('<').append(name).appendLine(">")
    block()
    appendIndent(level)
    append("</").append(name).appendLine(">")
}

private fun StringBuilder.appendTextElement(level: Int, name: String, value: String) {
    appendIndent(level)
    append('<').append(name).append('>')
    append(escapeXml(value))
    append("</").append(name).appendLine(">")
}

private fun StringBuilder.appendConfigurationElement(
    level: Int,
    name: String,
    implementation: String,
    defaultValue: String? = null,
    value: String,
) {
    appendIndent(level)
    append('<').append(name)
    append(" implementation=\"").append(escapeXml(implementation)).append('"')
    defaultValue?.let {
        append(" default-value=\"").append(escapeXml(it)).append('"')
    }
    append('>')
    append(escapeXml(value))
    append("</").append(name).appendLine(">")
}

private fun StringBuilder.appendParameter(
    level: Int,
    name: String,
    type: String,
    required: Boolean,
    editable: Boolean,
    description: String,
) {
    appendContainer(level, "parameter") {
        appendTextElement(level + 1, "name", name)
        appendTextElement(level + 1, "type", type)
        appendTextElement(level + 1, "required", required.toString())
        appendTextElement(level + 1, "editable", editable.toString())
        appendTextElement(level + 1, "description", description)
    }
}

private fun StringBuilder.appendIndent(level: Int) {
    repeat(level) {
        append("  ")
    }
}

private fun escapeXml(value: String): String =
    buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
