package io.github.lmliam.microsmith.resolve.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import java.util.ServiceLoader
import kotlin.reflect.KClass

class DomainResolverRegistry(
    resolvers: List<DomainResolver<*, *>> = loadDomainResolvers(),
) {
    private val resolversByAuthoringType: Map<KClass<out MicrosmithExtension>, List<DomainResolver<*, *>>> =
        indexResolvers(resolvers)

    fun resolve(extension: MicrosmithExtension): List<DomainResolver<MicrosmithExtension, ResolvedModel>> {
        return resolversByAuthoringType[extension::class]
            ?.map { it.cast() }
            .orEmpty()
    }

    private fun indexResolvers(
        resolvers: List<DomainResolver<*, *>>,
    ): Map<KClass<out MicrosmithExtension>, List<DomainResolver<*, *>>> {
        val byAuthoringType = resolvers.groupBy(DomainResolver<*, *>::authoringType)
        byAuthoringType.forEach { (type, registrations) ->
            val duplicateImplementations =
                registrations
                    .groupBy { it::class }
                    .filterValues { it.size > 1 }
                    .keys
                    .map { it.qualifiedName ?: it.toString() }
                    .sorted()
            require(duplicateImplementations.isEmpty()) {
                "Duplicate domain resolvers registered for authoring type ${formatType(type)}: " +
                    duplicateImplementations.joinToString(", ")
            }
        }

        return byAuthoringType.mapValues { (_, registrations) ->
            registrations.sortedWith(
                compareBy<DomainResolver<*, *>>(
                    { it.resolvedType.qualifiedName ?: it.resolvedType.toString() },
                    { it::class.qualifiedName ?: it::class.toString() },
                ),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun DomainResolver<*, *>.cast(): DomainResolver<MicrosmithExtension, ResolvedModel> =
        this as DomainResolver<MicrosmithExtension, ResolvedModel>

    private fun formatType(type: KClass<out MicrosmithExtension>): String = type.qualifiedName ?: type.toString()
}

private fun loadDomainResolvers(): List<DomainResolver<*, *>> =
    ServiceLoader.load(DomainResolver::class.java).iterator().asSequence().toList()
