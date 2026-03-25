package io.github.lmliam.microsmith.resolve.schemas.core

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import kotlin.reflect.KClass

@ServiceProvider(DomainResolver::class)
class SchemasResolver : DomainResolver<SchemasExtension, ResolvedSchemasModel> {
    override val authoringType: KClass<SchemasExtension> = SchemasExtension::class
    override val resolvedType: KClass<ResolvedSchemasModel> = ResolvedSchemasModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedSchemasModel = ResolvedSchemasModel(authoring.schemas)
}
