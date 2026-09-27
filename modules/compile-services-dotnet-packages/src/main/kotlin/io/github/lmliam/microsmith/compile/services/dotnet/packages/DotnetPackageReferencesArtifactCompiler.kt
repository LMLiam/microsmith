package io.github.lmliam.microsmith.compile.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReference
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.ServicesArtifactCompiler

@ServiceProvider(ArtifactCompiler::class)
class DotnetPackageReferencesArtifactCompiler : ServicesArtifactCompiler<DotnetPackageReferencesArtifact> {
    override val artifactType = DotnetPackageReferencesArtifact::class

    override fun compile(artifact: DotnetPackageReferencesArtifact): List<ArtifactContribution<out Artifact>> = listOf(
        MsBuildProjectContribution(
            artifactId = MsBuildProjectArtifactId(
                solutionName = artifact.solutionName,
                projectName = artifact.projectName,
                kind = MsBuildProjectKind.DirectoryBuildProps,
            ),
            items = artifact.packages.sortedBy(DotnetPackageReference::name).map { packageReference ->
                MsBuildItem(
                    itemName = MsBuildNames.PACKAGE_REFERENCE_ITEM,
                    include = packageReference.name,
                    attributes = packageReference.version
                        ?.let { mapOf(MsBuildNames.VERSION_ATTRIBUTE to it) }
                        .orEmpty(),
                )
            },
            origins = artifact.packages.mapTo(linkedSetOf()) { packageReference ->
                "services.${artifact.id.serviceName}.packages.${packageReference.name}"
            },
        ),
    )
}
