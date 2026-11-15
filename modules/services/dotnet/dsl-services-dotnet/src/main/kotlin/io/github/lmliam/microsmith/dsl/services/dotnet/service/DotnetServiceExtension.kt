package io.github.lmliam.microsmith.dsl.services.dotnet.service

import io.github.lmliam.microsmith.dsl.MergeableExtension
import io.github.lmliam.microsmith.dsl.services.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier
import kotlin.reflect.KClass

/** Per-service .NET configuration declared under `"ServiceName" { dotnet { ... } }`. */
public data class DotnetServiceExtension(
    public val target: DotnetTarget? = null,
    public val solution: String? = null,
    public val project: String? = null,
    public val models: Map<String, DotnetModel> = emptyMap(),
    public val model: DotnetServiceModel = DotnetServiceModel.empty(),
) : ServiceExtension, MergeableExtension<DotnetServiceExtension> {
    public fun findModel(name: String): DotnetModel? = models[name]

    public fun requireModel(name: String): DotnetModel {
        val normalized = validateDotnetIdentifier(name, "Model name")
        return findModel(normalized) ?: error("Dotnet model not found: $normalized")
    }

    public fun models(): Collection<DotnetModel> = models.values

    public fun <T : ServiceExtension> get(type: KClass<T>): T? = model.get(type)

    public inline fun <reified T : ServiceExtension> get(): T? = get(T::class)

    internal fun <T : ServiceExtension> with(type: KClass<T>, ext: T) = copy(model = model.with(type, ext))

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
