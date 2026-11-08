package io.github.lmliam.microsmith.resolve.services.dotnet

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetWorkspaceDomainResolver(
    private val workspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
) : DomainResolver<ServicesExtension, DotnetWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetWorkspace::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetWorkspace> {
        val workspace = workspaceResolver.resolve(authoring)
        return if (workspace.target != null || workspace.solutions.isNotEmpty() || workspace.services.isNotEmpty()) {
            DomainResolution.Success(workspace)
        } else {
            DomainResolution.NotApplicable
        }
    }
}
