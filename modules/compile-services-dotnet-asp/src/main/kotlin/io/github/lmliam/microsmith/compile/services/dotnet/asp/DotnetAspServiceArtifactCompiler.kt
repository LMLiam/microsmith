package io.github.lmliam.microsmith.compile.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler
import java.nio.charset.StandardCharsets
import java.nio.file.Path

@ServiceProvider(ArtifactCompiler::class)
class DotnetAspServiceArtifactCompiler : ServicesArtifactCompiler<DotnetAspServiceArtifact> {
    private val endpointRenderer = DotnetAspEndpointTextFileRenderer()

    override val artifactType = DotnetAspServiceArtifact::class

    override fun compile(artifact: DotnetAspServiceArtifact): List<ArtifactContribution<out Artifact>> = buildList {
        add(
            MsBuildProjectContribution(
                artifactId =
                MsBuildProjectArtifactId(
                    solutionName = artifact.id.solutionName,
                    projectName = artifact.id.projectName,
                    kind = MsBuildProjectKind.Project,
                ),
                projectAttributes = mapOf(MsBuildNames.SDK_ATTRIBUTE to "Microsoft.NET.Sdk.Web"),
                properties =
                mapOf(
                    MsBuildNames.IMPLICIT_USINGS_PROPERTY to "enable",
                    MsBuildNames.NULLABLE_PROPERTY to "enable",
                    MsBuildNames.TARGET_FRAMEWORK_PROPERTY to artifact.targetFrameworkMoniker,
                ),
            ),
        )
        add(textContribution(artifact, "appsettings.json", renderAppSettingsFile(artifact)))
        add(
            textContribution(
                artifact,
                "Properties/launchSettings.json",
                renderLaunchSettingsFile(artifact),
            ),
        )
        endpointRenderer.render(artifact).forEach { (relativePath, contents) ->
            add(textContribution(artifact, relativePath, contents))
        }
    }

    private fun textContribution(
        artifact: DotnetAspServiceArtifact,
        relativePath: String,
        contents: String,
    ): TextFileArtifactContribution = TextFileArtifactContribution(
        artifactId =
        TextFileArtifactId(
            relativePath = Path.of(relativePath),
            outputRoot = artifact.outputRoot,
        ),
        contents = contents,
    )

    private fun renderAppSettingsFile(artifact: DotnetAspServiceArtifact): String = renderTemplate(
        name = "appsettings.json.template",
        substitutions = mapOf(
            "{{SERVICE_NAME}}" to dotnetAspEscapeStringContents(artifact.serviceName),
        ),
    )

    private fun renderLaunchSettingsFile(artifact: DotnetAspServiceArtifact): String = renderTemplate(
        name = "launchSettings.json.template",
        substitutions = mapOf(
            "{{PROJECT_NAME}}" to artifact.id.projectName,
            "{{HTTP_PORT}}" to artifact.httpPort.toString(),
            "{{HTTPS_PORT}}" to artifact.httpsPort.toString(),
        ),
    )

    private fun renderTemplate(name: String, substitutions: Map<String, String>): String =
        substitutions.entries.fold(loadTemplate(name)) { rendered, (placeholder, replacement) ->
            rendered.replace(placeholder, replacement)
        }

    private fun loadTemplate(name: String): String = javaClass
        .getResourceAsStream("$TEMPLATE_RESOURCE_ROOT/$name")
        ?.readBytes()
        ?.toString(StandardCharsets.UTF_8)
        ?: error("Missing ASP.NET template resource '$name'.")

    private companion object {
        const val TEMPLATE_RESOURCE_ROOT =
            "/io/github/lmliam/microsmith/compile/services/dotnet/asp/templates"
    }
}
