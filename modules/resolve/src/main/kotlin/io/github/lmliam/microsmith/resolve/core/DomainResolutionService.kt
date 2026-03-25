package io.github.lmliam.microsmith.resolve.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.core.MicrosmithModel
import io.github.lmliam.microsmith.dsl.helpers.extensions

class DomainResolutionService(
    private val resolverRegistry: DomainResolverRegistry = DomainResolverRegistry(),
) {
    fun resolve(model: MicrosmithModel): List<ResolvedModel> {
        return model.extensions()
            .sortedBy { it::class.qualifiedName ?: it::class.toString() }
            .flatMap { extension ->
                resolverRegistry.resolve(extension).mapNotNull { resolver ->
                    resolver.resolveUnchecked(extension)
                }
            }
    }
}

@Suppress("UNCHECKED_CAST")
private fun DomainResolver<MicrosmithExtension, ResolvedModel>.resolveUnchecked(
    extension: MicrosmithExtension,
): ResolvedModel? = (this as DomainResolver<MicrosmithExtension, ResolvedModel>).resolve(extension)
