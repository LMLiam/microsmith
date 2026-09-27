package io.github.lmliam.microsmith.resolve.services
import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver

@ServiceProvider(DomainResolver::class)
class ServicesResolver : DomainResolver<ServicesExtension, ResolvedServicesModel> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = ResolvedServicesModel::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<ResolvedServicesModel> =
        DomainResolution.Success(
            ResolvedServicesModel(authoring.services),
        )
}
