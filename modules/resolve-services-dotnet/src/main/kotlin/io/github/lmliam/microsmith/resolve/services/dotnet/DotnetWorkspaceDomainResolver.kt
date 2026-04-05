package io.github.lmliam.microsmith.resolve.services.dotnet

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.core.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetWorkspaceDomainResolver(
    private val workspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
) : DomainResolver<ServicesExtension, DotnetWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetWorkspace::class

    override fun resolve(authoring: ServicesExtension): DotnetWorkspace? {
        val workspace = workspaceResolver.resolve(authoring)
        return workspace.takeIf { it.target != null || it.solutions.isNotEmpty() || it.services.isNotEmpty() }
    }
}
