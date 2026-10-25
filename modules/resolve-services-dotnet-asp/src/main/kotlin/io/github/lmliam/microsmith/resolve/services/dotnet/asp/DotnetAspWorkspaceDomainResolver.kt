package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetAspWorkspaceDomainResolver(
    private val workspaceResolver: DotnetAspWorkspaceResolver = DotnetAspWorkspaceResolver(),
) : DomainResolver<ServicesExtension, DotnetAspWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetAspWorkspace::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetAspWorkspace> {
        val workspace = workspaceResolver.resolve(authoring)
        return if (workspace.servicesByName.isEmpty()) {
            DomainResolution.NotApplicable
        } else {
            DomainResolution.Success(workspace)
        }
    }
}
