package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
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
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetPluginScope

@Inject
@ContributesIntoSet(DotnetPluginScope::class, binding = binding<ArtifactCompiler<*>>())
internal class MsBuildProjectArtifactCompiler(
    private val xmlRenderer: MsBuildProjectXmlRenderer,
) : ServicesArtifactCompiler<MsBuildProjectArtifact> {
    override val artifactType = MsBuildProjectArtifact::class

    override fun compile(artifact: MsBuildProjectArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId =
                    TextFileArtifactId(
                        relativePath = projectRelativePath(artifact),
                        outputRoot = artifact.outputRoot(),
                    ),
                contents = xmlRenderer.render(artifact),
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
