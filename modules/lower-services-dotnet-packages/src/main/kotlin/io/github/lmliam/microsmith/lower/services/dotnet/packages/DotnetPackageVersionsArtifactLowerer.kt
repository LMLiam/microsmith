package io.github.lmliam.microsmith.lower.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.lower.core.ArtifactLowerer
import kotlin.reflect.KClass

@ServiceProvider(ArtifactLowerer::class)
class DotnetPackageVersionsArtifactLowerer : ArtifactLowerer<DotnetPackageVersionsArtifact> {
    override val artifactType: KClass<DotnetPackageVersionsArtifact> = DotnetPackageVersionsArtifact::class

    override fun lower(artifact: DotnetPackageVersionsArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(
            MsBuildProjectContribution(
                artifactId = MsBuildProjectArtifactId(
                    solutionName = artifact.id.solutionName,
                    kind = MsBuildProjectKind.DirectoryPackagesProps,
                ),
                properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                items = artifact.packages.toSortedMap().map { (packageName, version) ->
                    MsBuildItem(
                        type = "PackageVersion",
                        include = packageName,
                        metadata = mapOf("Version" to version),
                    )
                },
            ),
        )
    }
}
