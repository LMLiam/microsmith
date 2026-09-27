package io.github.lmliam.microsmith.resolve.schemas

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver

@ServiceProvider(DomainResolver::class)
class SchemasResolver : DomainResolver<SchemasExtension, ResolvedSchemasModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedSchemasModel::class

    override fun resolve(authoring: SchemasExtension): DomainResolution<ResolvedSchemasModel> =
        DomainResolution.Success(
            ResolvedSchemasModel(authoring.schemas),
        )
}
