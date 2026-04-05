package io.github.lmliam.microsmith.resolve.services.dotnet.packages

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.core.DomainResolver

@ServiceProvider(DomainResolver::class)
class DotnetPackageWorkspaceDomainResolver(
    private val workspaceResolver: DotnetPackageWorkspaceResolver = DotnetPackageWorkspaceResolver(),
) : DomainResolver<ServicesExtension, DotnetPackageWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetPackageWorkspace::class

    override fun resolve(authoring: ServicesExtension): DotnetPackageWorkspace? {
        val workspace = workspaceResolver.resolve(authoring)
        return workspace.takeIf {
            it.solutionsByName.isNotEmpty() || it.servicesByName.isNotEmpty()
        }
    }
}
