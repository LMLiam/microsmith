package io.github.lmliam.microsmith.compile.services.dotnet.packages

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions.DotnetPackageVersion
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.ServicesArtifactCompiler
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackagesPluginScope

@Inject
@ContributesIntoSet(DotnetPackagesPluginScope::class, binding = binding<ArtifactCompiler<*>>())
internal class DotnetPackageVersionsArtifactCompiler : ServicesArtifactCompiler<DotnetPackageVersionsArtifact> {
    override val artifactType = DotnetPackageVersionsArtifact::class

    override fun compile(artifact: DotnetPackageVersionsArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            MsBuildProjectContribution(
                artifactId = MsBuildProjectArtifactId.DirectoryPackagesProps(solutionName = artifact.id.solutionName),
                properties = mapOf(MsBuildNames.MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY to "true"),
                items =
                    artifact.packages.sortedBy(DotnetPackageVersion::name).map { packageVersion ->
                        MsBuildItem(
                            itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                            include = packageVersion.name,
                            attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to packageVersion.version),
                        )
                    },
            ),
        )
}
