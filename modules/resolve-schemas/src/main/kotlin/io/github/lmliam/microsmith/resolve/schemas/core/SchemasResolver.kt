package io.github.lmliam.microsmith.resolve.schemas.core

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.resolve.core.DomainResolver

@ServiceProvider(DomainResolver::class)
class SchemasResolver : DomainResolver<SchemasExtension, ResolvedSchemasModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedSchemasModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedSchemasModel = ResolvedSchemasModel(authoring.schemas)
}
