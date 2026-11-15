package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspPluginScope

@Inject
@ContributesIntoSet(DotnetAspPluginScope::class, binding = binding<DomainResolver<*, *>>())
internal class DotnetAspWorkspaceDomainResolver(
    private val workspaceResolver: DotnetAspWorkspaceResolver,
) : DomainResolver<ServicesExtension, DotnetAspWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetAspWorkspace::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetAspWorkspace> =
        workspaceResolver
            .resolve(authoring)
            .fold(
                ifLeft = { DomainResolution.Failure(it) },
                ifRight = { workspace ->
                    if (workspace.servicesByName.isEmpty()) {
                        DomainResolution.NotApplicable
                    } else {
                        DomainResolution.Success(workspace)
                    }
                },
            )
}
