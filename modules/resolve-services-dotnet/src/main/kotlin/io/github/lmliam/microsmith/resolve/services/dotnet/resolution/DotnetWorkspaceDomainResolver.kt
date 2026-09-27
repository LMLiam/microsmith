package io.github.lmliam.microsmith.resolve.services.dotnet.resolution

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetWorkspaceDomainResolver(
    private val workspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver()
) : DomainResolver<ServicesExtension, DotnetWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetWorkspace::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetWorkspace> =
        workspaceResolver
            .resolve(authoring)
            .fold(
                ifLeft = { DomainResolution.Failure(it) },
                ifRight = { workspace ->
                    if (workspace.solutions.isNotEmpty() || workspace.services.isNotEmpty()) {
                        DomainResolution.Success(workspace)
                    } else {
                        DomainResolution.NotApplicable
                    }
                },
            )
}
