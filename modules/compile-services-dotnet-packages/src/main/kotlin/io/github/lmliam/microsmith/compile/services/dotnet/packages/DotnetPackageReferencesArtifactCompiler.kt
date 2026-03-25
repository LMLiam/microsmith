package io.github.lmliam.microsmith.compile.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler
import kotlin.reflect.KClass

@ServiceProvider(ArtifactCompiler::class)
class DotnetPackageReferencesArtifactCompiler : ServicesArtifactCompiler<DotnetPackageReferencesArtifact> {
    override val artifactType: KClass<DotnetPackageReferencesArtifact> = DotnetPackageReferencesArtifact::class

    override fun compile(artifact: DotnetPackageReferencesArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(
            MsBuildProjectContribution(
                artifactId = MsBuildProjectArtifactId(
                    solutionName = artifact.solutionName,
                    projectName = artifact.projectName,
                    kind = MsBuildProjectKind.PackageReferencesProps,
                ),
                items = artifact.packages.sorted().map { packageName ->
                    MsBuildItem(
                        type = "PackageReference",
                        include = packageName,
                    )
                },
            ),
        )
    }
}
