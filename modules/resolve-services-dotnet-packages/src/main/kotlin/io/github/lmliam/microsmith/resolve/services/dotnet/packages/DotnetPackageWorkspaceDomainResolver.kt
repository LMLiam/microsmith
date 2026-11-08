package io.github.lmliam.microsmith.resolve.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetPackageWorkspaceDomainResolver(
    private val workspaceResolver: DotnetPackageWorkspaceResolver = DotnetPackageWorkspaceResolver(),
) : DomainResolver<ServicesExtension, DotnetPackageWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetPackageWorkspace::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetPackageWorkspace> {
        val workspace = workspaceResolver.resolve(authoring)
        return if (workspace.solutionsByName.isEmpty() && workspace.servicesByName.isEmpty()) {
            DomainResolution.NotApplicable
        } else {
            DomainResolution.Success(workspace)
        }
    }
}
