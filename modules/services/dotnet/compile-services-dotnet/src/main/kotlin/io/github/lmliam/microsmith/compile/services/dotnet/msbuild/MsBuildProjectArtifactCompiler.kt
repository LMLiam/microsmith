package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.dotnetProjectOutputRoot
import io.github.lmliam.microsmith.artifact.services.dotnet.dotnetSolutionOutputRoot
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.ServicesArtifactCompiler

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
            ),
        )

    private fun MsBuildProjectArtifact.outputRoot(): RelativePath =
        when (val id = id) {
            is MsBuildProjectArtifactId.DirectoryPackagesProps -> dotnetSolutionOutputRoot(id.solutionName)
            is MsBuildProjectArtifactId.DirectoryBuildProps -> dotnetProjectOutputRoot(id.solutionName, id.projectName)
            is MsBuildProjectArtifactId.Project -> dotnetProjectOutputRoot(id.solutionName, id.projectName)
        }

    private fun projectRelativePath(artifact: MsBuildProjectArtifact): RelativePath =
        when (val id = artifact.id) {
            is MsBuildProjectArtifactId.DirectoryPackagesProps -> RelativePath.of("Directory.Packages.props")
            is MsBuildProjectArtifactId.DirectoryBuildProps -> RelativePath.of("Directory.Build.props")
            is MsBuildProjectArtifactId.Project -> RelativePath.of("${id.projectName}.csproj")
        }
}
