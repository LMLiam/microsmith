package io.github.lmliam.microsmith.compile.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
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
        val serviceOrigin = setOf("services.${artifact.serviceName}")
        val requestBindings = artifact.requestBindings()
        val headerBindings = artifact.headerBindings()
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
            add(textContribution(artifact, "Program.cs", DotnetAspProjectRenderer.renderProgramFile(), serviceOrigin))
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
                    "Controllers/${artifact.serviceName}Controller.cs",
                    DotnetAspProjectRenderer.renderControllerFile(artifact),
                    controllerOrigins,
                ),
            )
            artifact.contractModels.distinctBy(DotnetAspModelArtifact::typeName).forEach { model ->
                add(
                    textContribution(
                        artifact,
                        "Models/${model.typeName}.cs",
                        DotnetAspProjectRenderer.renderModelFile(artifact.id.projectName, model),
                        model.origins,
                    ),
                )
            }
            requestBindings.forEach { binding ->
                add(
                    textContribution(
                        artifact,
                        "Bindings/${binding.typeName}.cs",
                        DotnetAspProjectRenderer.renderRequestBindingFile(artifact.id.projectName, binding),
                        binding.origins,
                    ),
                )
            }
            headerBindings.forEach { binding ->
                add(
                    textContribution(
                        artifact,
                        "Bindings/${binding.typeName}.cs",
                        DotnetAspProjectRenderer.renderHeadersBindingFile(artifact.id.projectName, binding),
                        binding.origins,
                    ),
                )
            }
            add(
                textContribution(
                    artifact,
                    "Generated/MicrosmithRequestParser.cs",
                    DotnetAspProjectRenderer.renderRequestParserFile(artifact.id.projectName),
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

    private fun DotnetAspServiceArtifact.requestBindings(): List<DotnetAspRequestBindingArtifact> = endpoints
        .flatMap { endpoint ->
            listOfNotNull(endpoint.bindings.path, endpoint.bindings.query)
        }.distinctBy(DotnetAspRequestBindingArtifact::typeName)

    private fun DotnetAspServiceArtifact.headerBindings(): List<DotnetAspHeadersBindingArtifact> = endpoints
        .map(DotnetAspEndpointArtifact::bindings)
        .mapNotNull { it.headers }
        .distinctBy(DotnetAspHeadersBindingArtifact::typeName)
}
