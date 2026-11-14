package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.ServicesArtifactCompiler
import java.nio.file.Path

@ServiceProvider(ArtifactCompiler::class)
class MsBuildProjectArtifactCompiler : ServicesArtifactCompiler<MsBuildProjectArtifact> {
    override val artifactType = MsBuildProjectArtifact::class

    override fun compile(artifact: MsBuildProjectArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId =
                    TextFileArtifactId(
                        relativePath = projectRelativePath(artifact),
                        outputRoot = artifact.outputRoot(),
                    ),
                contents = MsBuildProjectXmlRenderer.render(artifact),
            )
        )

    private fun MsBuildProjectArtifact.outputRoot(): Path =
        when (val id = id) {
            is MsBuildProjectArtifactId.DirectoryPackagesProps -> dotnetOutputRoot.resolve(id.solutionName)
            is MsBuildProjectArtifactId.DirectoryBuildProps ->
                dotnetOutputRoot.resolve(id.solutionName).resolve(id.projectName)
            is MsBuildProjectArtifactId.Project -> dotnetOutputRoot.resolve(id.solutionName).resolve(id.projectName)
        }

    private fun projectRelativePath(artifact: MsBuildProjectArtifact): Path =
        when (val id = artifact.id) {
            is MsBuildProjectArtifactId.DirectoryPackagesProps -> Path.of("Directory.Packages.props")
            is MsBuildProjectArtifactId.DirectoryBuildProps -> Path.of("Directory.Build.props")
            is MsBuildProjectArtifactId.Project -> Path.of("${id.projectName}.csproj")
        }

    private companion object {
        val dotnetOutputRoot: Path = Path.of("dotnet")
    }
}
