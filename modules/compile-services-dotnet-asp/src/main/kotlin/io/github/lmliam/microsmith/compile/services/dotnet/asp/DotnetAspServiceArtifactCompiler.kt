package io.github.lmliam.microsmith.compile.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler
import java.nio.file.Path

@ServiceProvider(ArtifactCompiler::class)
class DotnetAspServiceArtifactCompiler : ServicesArtifactCompiler<DotnetAspServiceArtifact> {
    private companion object {
        const val FIRST_NON_PRINTABLE_ASCII_CODE_POINT = 0x20
    }

    override val artifactType = DotnetAspServiceArtifact::class

    override fun compile(artifact: DotnetAspServiceArtifact): List<ArtifactContribution<out Artifact>> {
        validateEndpointGenerationInputs(artifact)
        val serviceOrigin = setOf("services.${artifact.serviceName}")
        val requestModelOrigins = serviceOrigin +
            collectRequestBindings(artifact).flatMapTo(linkedSetOf()) { it.origins } +
            collectHeaderBindings(artifact).flatMapTo(linkedSetOf()) { it.origins } +
            artifact.endpoints.mapNotNull { endpoint ->
                endpoint.bindings.body
                    ?.takeIf { it.locality == io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality.INLINE }
                    ?.origins
            }.flatten()
        val responseModelOrigins = serviceOrigin +
            artifact.endpoints.flatMapTo(linkedSetOf()) { endpoint ->
                endpoint.responses.flatMap { response ->
                    response.origins + response.model.origins
                }
            }
        val controllerOrigins = serviceOrigin +
            artifact.endpoints.flatMapTo(linkedSetOf()) { endpoint ->
                endpoint.origins +
                    endpoint.responses.flatMapTo(linkedSetOf()) { it.origins } +
                    listOfNotNull(
                        endpoint.bindings.path?.origins,
                        endpoint.bindings.query?.origins,
                        endpoint.bindings.headers?.origins,
                        endpoint.bindings.body?.origins,
                    ).flatten()
            }

        return buildList {
            add(
                MsBuildProjectContribution(
                    artifactId = MsBuildProjectArtifactId(
                        solutionName = artifact.id.solutionName,
                        projectName = artifact.id.projectName,
                        kind = MsBuildProjectKind.Project,
                    ),
                    projectAttributes = mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web"),
                    properties = mapOf(
                        MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                        MsBuildNames.NULLABLE_PROPERTY to "enable",
                        MsBuildNames.TARGET_FRAMEWORK_PROPERTY to artifact.targetFrameworkMoniker,
                    ),
                    origins = serviceOrigin,
                ),
            )
            add(
                textContribution(
                    artifact,
                    "Program.cs",
                    DotnetAspProjectRenderer.renderProgramFile(artifact),
                    serviceOrigin,
                ),
            )
            add(textContribution(artifact, "appsettings.json", renderAppSettingsFile(artifact), serviceOrigin))
            add(
                textContribution(
                    artifact,
                    "Properties/launchSettings.json",
                    renderLaunchSettingsFile(artifact),
                    serviceOrigin,
                ),
            )
            add(
                textContribution(
                    artifact,
                    "Generated/Hosting/MicrosmithHostingExtensions.cs",
                    DotnetAspProjectRenderer.renderHostingExtensionsFile(artifact),
                    serviceOrigin,
                ),
            )
            add(
                textContribution(
                    artifact,
                    "Generated/Contracts/ServiceModels.cs",
                    DotnetAspProjectRenderer.renderServiceModelsFile(artifact),
                    artifact.contractModels
                        .distinctBy(DotnetAspModelArtifact::typeName)
                        .filter { it.locality == io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality.SHARED }
                        .flatMapTo(linkedSetOf()) { it.origins } + serviceOrigin,
                ),
            )
            add(
                textContribution(
                    artifact,
                    "Generated/Contracts/RequestModels.cs",
                    DotnetAspProjectRenderer.renderRequestModelsFile(artifact),
                    requestModelOrigins,
                ),
            )
            add(
                textContribution(
                    artifact,
                    "Generated/Contracts/ResponseModels.cs",
                    DotnetAspProjectRenderer.renderResponseModelsFile(artifact),
                    responseModelOrigins,
                ),
            )
            add(
                textContribution(
                    artifact,
                    microsmithControllerBaseRelativePath(),
                    DotnetAspProjectRenderer.renderMicrosmithControllerBaseFile(artifact),
                    serviceOrigin,
                ),
            )
            add(
                textContribution(
                    artifact,
                    controllerBaseRelativePath(artifact),
                    DotnetAspProjectRenderer.renderControllerBaseFile(artifact),
                    controllerOrigins,
                ),
            )
        }
    }

    private fun textContribution(
        artifact: DotnetAspServiceArtifact,
        relativePath: String,
        contents: String,
        origins: Set<String>,
    ): TextFileArtifactContribution = TextFileArtifactContribution(
        artifactId = TextFileArtifactId(
            relativePath = Path.of(relativePath),
            outputRoot = artifact.outputRoot,
        ),
        contents = contents,
        origins = origins,
    )

    private fun renderAppSettingsFile(artifact: DotnetAspServiceArtifact): String = """
        {
          "Microsmith": {
            "ServiceName": "${escapeJsonString(artifact.serviceName)}"
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

    private fun renderLaunchSettingsFile(artifact: DotnetAspServiceArtifact): String = """
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

    private fun escapeJsonString(value: String): String {
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

}
