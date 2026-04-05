package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.core.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetAspWorkspaceDomainResolver(
    private val workspaceResolver: DotnetAspWorkspaceResolver = DotnetAspWorkspaceResolver(),
) : DomainResolver<ServicesExtension, DotnetAspWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetAspWorkspace::class

    override fun resolve(authoring: ServicesExtension): DotnetAspWorkspace? {
        return workspaceResolver.resolve(authoring).takeIf { it.servicesByName.isNotEmpty() }
    }
}
