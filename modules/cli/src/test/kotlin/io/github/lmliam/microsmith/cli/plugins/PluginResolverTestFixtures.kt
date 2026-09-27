package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

internal fun fileRepositoryAllowedPolicy(vararg additionalAllowedRepositories: String): RepositoryAllowlistPolicy =
    RepositoryAllowlistPolicy(
        allowedRepositories =
        (listOf(MAVEN_CENTRAL_REPOSITORY) + additionalAllowedRepositories)
            .map(::normalizeRepositoryUri)
            .toSet(),
        allowFileRepositories = true,
    )

internal fun publishMavenArtifact(
    repositoryRoot: Path,
    coordinate: String,
    dependencies: List<String> = emptyList(),
    dependencyScopes: Map<String, String> = emptyMap(),
    jarContents: ByteArray = "plugin-jar-contents".toByteArray(),
) {
    val parsed = parseCoordinate(coordinate)
    val base =
        repositoryRoot
            .resolve(parsed.group.replace('.', '/'))
            .resolve(parsed.artifact)
            .resolve(parsed.version)
    base.createDirectories()

    val pomPath = base.resolve("${parsed.artifact}-${parsed.version}.pom")
    val dependenciesBlock =
        if (dependencies.isEmpty()) {
            ""
        } else {
            dependencies.joinToString(
                separator = "\n",
                prefix = "<dependencies>\n",
                postfix = "\n</dependencies>",
            ) { dependency ->
                val dep = parseCoordinate(dependency)
                val scopeXml =
                    dependencyScopes[dependency]
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
                        ?.let { scope -> "\n  <scope>$scope</scope>" }
                        .orEmpty()
                """
                <dependency>
                  <groupId>${dep.group}</groupId>
                  <artifactId>${dep.artifact}</artifactId>
                  <version>${dep.version}</version>
                  $scopeXml
                </dependency>
                """.trimIndent()
            }
        }
    val pomXml =
        """
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>
          <groupId>${parsed.group}</groupId>
          <artifactId>${parsed.artifact}</artifactId>
          <version>${parsed.version}</version>
          <packaging>jar</packaging>
          $dependenciesBlock
        </project>
        """.trimIndent()
    pomPath.writeText(pomXml)

    val jarPath = base.resolve("${parsed.artifact}-${parsed.version}.jar")
    jarPath.writeBytes(jarContents)
}
