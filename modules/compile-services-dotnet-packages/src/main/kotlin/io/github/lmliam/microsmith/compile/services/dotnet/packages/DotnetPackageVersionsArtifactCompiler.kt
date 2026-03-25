package io.github.lmliam.microsmith.compile.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler
import kotlin.reflect.KClass

@ServiceProvider(ArtifactCompiler::class)
class DotnetPackageVersionsArtifactCompiler : ServicesArtifactCompiler<DotnetPackageVersionsArtifact> {
    override val artifactType: KClass<DotnetPackageVersionsArtifact> = DotnetPackageVersionsArtifact::class

    override fun compile(artifact: DotnetPackageVersionsArtifact): List<ArtifactContribution<out Artifact>> {
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
