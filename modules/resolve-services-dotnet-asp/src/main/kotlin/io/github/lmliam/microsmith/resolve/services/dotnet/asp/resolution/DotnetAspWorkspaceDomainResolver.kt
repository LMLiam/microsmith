package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetAspWorkspaceDomainResolver(
    private val workspaceResolver: DotnetAspWorkspaceResolver = DotnetAspWorkspaceResolver()
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
