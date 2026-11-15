package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifactFactory
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifactId
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspPluginScope
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspWorkspace

@Inject
@ContributesIntoSet(DotnetAspPluginScope::class, binding = binding<ArtifactContributor<*>>())
internal class DotnetAspArtifactContributor : ArtifactContributor<DotnetAspWorkspace> {
    override val resolvedType = DotnetAspWorkspace::class

    override fun contribute(model: DotnetAspWorkspace): List<ArtifactContribution<*>> =
        model.servicesByName.values
            .map { service -> service to DotnetAspServiceArtifactId(service.solutionName, service.projectName) }
            .sortedWith(
                compareBy(
                    { (_, artifactId) -> artifactId.solutionName },
                    { (_, artifactId) -> artifactId.projectName },
                ),
            )
            .map { (service, artifactId) -> DotnetAspServiceArtifactFactory(service, artifactId).createContribution() }
}
