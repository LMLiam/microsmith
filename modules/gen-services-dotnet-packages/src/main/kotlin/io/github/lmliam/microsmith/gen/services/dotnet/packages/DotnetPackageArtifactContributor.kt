package io.github.lmliam.microsmith.gen.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectContribution
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackageWorkspace
import java.nio.file.Path
import kotlin.reflect.KClass

@ServiceProvider(ArtifactContributor::class)
class DotnetPackageArtifactContributor : ArtifactContributor<DotnetPackageWorkspace> {
    override val resolvedType: KClass<DotnetPackageWorkspace> = DotnetPackageWorkspace::class

    override fun contribute(model: DotnetPackageWorkspace): List<ArtifactContribution<*>> {
        val solutionContributions =
            model.solutions.values
                .sortedBy { it.name }
                .map { solution ->
                    MsBuildProjectContribution(
                        artifactId = MsBuildProjectArtifactId(
                            relativePath = Path.of("Directory.Packages.props"),
                            outputRoot = Path.of("dotnet", solution.name),
                        ),
                        properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                        items =
                        solution.packages
                            .toSortedMap()
                            .map { (packageName, version) ->
                                MsBuildItem(
                                    type = "PackageVersion",
                                    include = packageName,
                                    metadata = mapOf("Version" to version),
                                )
                            },
                    )
                }

        val serviceContributions =
            model.services.values
                .sortedBy { it.name }
                .map { service ->
                    MsBuildProjectContribution(
                        artifactId = MsBuildProjectArtifactId(
                            relativePath = Path.of("PackageReferences.props"),
                            outputRoot = Path.of("dotnet", service.solution, service.project),
                        ),
                        items =
                        service.packages
                            .toSortedMap()
                            .map { (packageName, _) ->
                                MsBuildItem(
                                    type = "PackageReference",
                                    include = packageName,
                                )
                            },
                    )
                }

        return solutionContributions + serviceContributions
    }
}
