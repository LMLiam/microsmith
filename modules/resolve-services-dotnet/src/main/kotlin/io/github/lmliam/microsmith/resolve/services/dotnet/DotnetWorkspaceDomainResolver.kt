package io.github.lmliam.microsmith.resolve.services.dotnet

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import kotlin.reflect.KClass

@ServiceProvider(DomainResolver::class)
class DotnetWorkspaceDomainResolver(
    private val workspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
) : DomainResolver<ServicesExtension, DotnetWorkspace> {
    override val authoringType: KClass<ServicesExtension> = ServicesExtension::class
    override val resolvedType: KClass<DotnetWorkspace> = DotnetWorkspace::class

    override fun resolve(authoring: ServicesExtension): DotnetWorkspace? {
        val workspace = workspaceResolver.resolve(authoring)
        return workspace.takeIf { it.target != null || it.solutions.isNotEmpty() || it.services.isNotEmpty() }
    }
}
