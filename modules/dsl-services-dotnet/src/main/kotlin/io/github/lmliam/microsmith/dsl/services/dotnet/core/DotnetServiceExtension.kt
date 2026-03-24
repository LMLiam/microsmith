package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import kotlin.reflect.KClass

/**
 * Per-service .NET configuration declared under `"ServiceName" { dotnet { ... } }`.
 */
data class DotnetServiceExtension(
    val target: DotnetTarget? = null,
    val solution: String? = null,
    val project: String? = null,
    val models: Map<String, DotnetModel> = emptyMap(),
    val model: DotnetServiceModel = DotnetServiceModel.empty(),
) : ServiceExtension, MergeableExtension<DotnetServiceExtension> {
    fun findModel(name: String) = models[name]

    fun requireModel(name: String): DotnetModel {
        val normalized = validateDotnetIdentifier(name, "Model name")
        return findModel(normalized) ?: error("Dotnet model not found: $normalized")
    }

    fun allModels() = models.values

    @Suppress("UNCHECKED_CAST")
    fun <T : ServiceExtension> get(type: KClass<T>) = model.get(type) as? T?

    inline fun <reified T : ServiceExtension> get(): T? = get(T::class)

    internal fun <T : ServiceExtension> with(type: KClass<T>, ext: T) = copy(
        model = model.with(type, ext),
    )

    override fun merge(other: DotnetServiceExtension): DotnetServiceExtension {
        val collisions = other.models.keys.filter { it in models }.sorted()

        require(collisions.isEmpty()) {
            "Duplicate .NET model registration while merging service configuration: ${collisions.joinToString(", ")}"
        }

        return copy(
            target = other.target ?: target,
            solution = other.solution ?: solution,
            project = other.project ?: project,
            models = models + other.models,
            model = model.merge(other.model),
        )
    }
}
