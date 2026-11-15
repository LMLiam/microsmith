package io.github.lmliam.microsmith.dsl.services.dotnet.defaults

import io.github.lmliam.microsmith.dsl.MergeableExtension
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.solution.DotnetSolution
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetQualifiedIdentifier
import kotlin.reflect.KClass

/** Shared .NET defaults declared under `services { dotnet { ... } }`. */
public data class DotnetDefaultsExtension(
    public val target: DotnetTarget? = null,
    public val solutions: Map<String, DotnetSolution> = emptyMap(),
    public val model: DotnetDefaultsModel = DotnetDefaultsModel.empty(),
) : MicrosmithExtension, MergeableExtension<DotnetDefaultsExtension> {
    public fun findSolution(name: String): DotnetSolution? = solutions[name]

    public fun requireSolution(name: String): DotnetSolution {
        val normalized = validateDotnetQualifiedIdentifier(name, "Solution name")
        return findSolution(normalized) ?: error("Dotnet solution not found: $normalized")
    }

    public fun allSolutions(): Collection<DotnetSolution> = solutions.values

    public fun <T : MicrosmithExtension> get(type: KClass<T>): T? = model.get(type)

    public inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    internal fun <T : MicrosmithExtension> with(type: KClass<T>, ext: T) = copy(model = model.with(type, ext))

    override fun merge(other: DotnetDefaultsExtension): DotnetDefaultsExtension {
        val mergedSolutions = linkedMapOf<String, DotnetSolution>()

        solutions.values.forEach { solution -> mergedSolutions[solution.name] = solution }

        other.solutions.values.forEach { solution ->
            mergedSolutions[solution.name] = mergedSolutions[solution.name]?.merge(solution) ?: solution
        }

        return copy(
            target = other.target ?: target,
            solutions = mergedSolutions,
            model = model.merge(other.model),
        )
    }
}
