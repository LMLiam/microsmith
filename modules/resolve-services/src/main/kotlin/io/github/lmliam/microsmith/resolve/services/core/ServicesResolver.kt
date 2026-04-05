package io.github.lmliam.microsmith.resolve.services.core

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.resolve.core.DomainResolver

@ServiceProvider(DomainResolver::class)
class ServicesResolver : DomainResolver<ServicesExtension, ResolvedServicesModel> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = ResolvedServicesModel::class

    override fun resolve(authoring: ServicesExtension): ResolvedServicesModel {
        return ResolvedServicesModel(authoring.services)
    }
}
