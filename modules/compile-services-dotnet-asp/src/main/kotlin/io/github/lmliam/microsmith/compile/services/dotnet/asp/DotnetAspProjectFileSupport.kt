package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import java.nio.file.Path

private const val FIRST_NON_PRINTABLE_ASCII_CODE_POINT = 0x20

internal fun DotnetAspServiceArtifact.msBuildProjectArtifactId(kind: MsBuildProjectKind): MsBuildProjectArtifactId =
    MsBuildProjectArtifactId(
        solutionName = id.solutionName,
        projectName = id.projectName,
        kind = kind,
    )

internal fun DotnetAspServiceArtifact.textContribution(
    relativePath: String,
    contents: String,
    origins: Set<String>,
): TextFileArtifactContribution = TextFileArtifactContribution(
    artifactId = TextFileArtifactId(
        relativePath = Path.of(relativePath),
        outputRoot = outputRoot,
    ),
    contents = contents,
    origins = origins,
)

internal fun renderDotnetAspAppSettingsFile(artifact: DotnetAspServiceArtifact): String = """
    {
      "Microsmith": {
        "ServiceName": "${escapeDotnetAspJsonString(artifact.serviceName)}"
      },
      "Logging": {
        "LogLevel": {
          "Default": "Information",
          "Microsoft.AspNetCore": "Warning"
        }
      },
      "AllowedHosts": "*"
    }
""".trimIndent()

internal fun renderDotnetAspLaunchSettingsFile(artifact: DotnetAspServiceArtifact): String = """
    {
      "${'$'}schema": "http://json.schemastore.org/launchsettings.json",
      "profiles": {
        "${artifact.id.projectName}": {
          "commandName": "Project",
          "dotnetRunMessages": true,
          "launchBrowser": false,
          "applicationUrl": "http://localhost:${artifact.httpPort};https://localhost:${artifact.httpsPort}",
          "environmentVariables": {
            "ASPNETCORE_ENVIRONMENT": "Development"
          }
        }
      }
    }
""".trimIndent()

private fun escapeDotnetAspJsonString(value: String): String {
    val escaped = StringBuilder(value.length)
    value.forEach { char ->
        when (char) {
            '\\' -> escaped.append("\\\\")
            '"' -> escaped.append("\\\"")
            '\b' -> escaped.append("\\b")
            '\u000C' -> escaped.append("\\f")
            '\n' -> escaped.append("\\n")
            '\r' -> escaped.append("\\r")
            '\t' -> escaped.append("\\t")
            else -> {
                if (char.code < FIRST_NON_PRINTABLE_ASCII_CODE_POINT) {
                    escaped.append("\\u%04x".format(char.code))
                } else {
                    escaped.append(char)
                }
            }
        }
    }
    return escaped.toString()
}
