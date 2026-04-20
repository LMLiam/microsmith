package io.github.lmliam.microsmith.compile.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildNames
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersion
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler

@ServiceProvider(ArtifactCompiler::class)
class DotnetPackageVersionsArtifactCompiler : ServicesArtifactCompiler<DotnetPackageVersionsArtifact> {
    override val artifactType = DotnetPackageVersionsArtifact::class

    override fun compile(artifact: DotnetPackageVersionsArtifact): List<ArtifactContribution<out Artifact>> = listOf(
        MsBuildProjectContribution(
            artifactId = MsBuildProjectArtifactId(
                solutionName = artifact.id.solutionName,
                kind = MsBuildProjectKind.DirectoryPackagesProps,
            ),
            properties = mapOf(MsBuildNames.MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY to "true"),
            items = artifact.packages.sortedBy(DotnetPackageVersion::name).map { packageVersion ->
                MsBuildItem(
                    itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                    include = packageVersion.name,
                    attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to packageVersion.version),
                )
            },
            origins = artifact.packages.mapTo(linkedSetOf()) { packageVersion ->
                "services.solutions.${artifact.id.solutionName}.packages.${packageVersion.name}"
            },
        ),
    )
}
