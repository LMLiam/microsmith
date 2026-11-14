package io.github.lmliam.microsmith.compile.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReference
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferenceVersion
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.ServicesArtifactCompiler

@ServiceProvider(ArtifactCompiler::class)
class DotnetPackageReferencesArtifactCompiler : ServicesArtifactCompiler<DotnetPackageReferencesArtifact> {
    override val artifactType = DotnetPackageReferencesArtifact::class

    override fun compile(artifact: DotnetPackageReferencesArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            MsBuildProjectContribution(
                artifactId =
                    MsBuildProjectArtifactId.DirectoryBuildProps(
                        solutionName = artifact.solutionName,
                        projectName = artifact.projectName,
                    ),
                items =
                    artifact.packages.sortedBy(DotnetPackageReference::name).map { packageReference ->
                        MsBuildItem(
                            itemName = MsBuildNames.PACKAGE_REFERENCE_ITEM,
                            include = packageReference.name,
                            attributes =
                                when (val version = packageReference.version) {
                                    DotnetPackageReferenceVersion.Central -> emptyMap()
                                    is DotnetPackageReferenceVersion.Explicit ->
                                        mapOf(MsBuildNames.VERSION_ATTRIBUTE to version.version)
                                },
                        )
                    },
            ),
        )
}
