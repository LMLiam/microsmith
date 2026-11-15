package io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackagesPluginScope

@Inject
@ContributesIntoSet(DotnetPackagesPluginScope::class, binding = binding<DomainResolver<*, *>>())
internal class DotnetPackageWorkspaceDomainResolver(
    private val workspaceResolver: DotnetPackageWorkspaceResolver,
) : DomainResolver<ServicesExtension, DotnetPackageWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetPackageWorkspace::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetPackageWorkspace> =
        workspaceResolver
            .resolve(authoring)
            .fold(
                ifLeft = { DomainResolution.Failure(it) },
                ifRight = { workspace ->
                    if (workspace.solutionsByName.isEmpty() && workspace.servicesByName.isEmpty()) {
                        DomainResolution.NotApplicable
                    } else {
                        DomainResolution.Success(workspace)
                    }
                },
            )
}
