package io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolution
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetQualifiedIdentifier
import kotlin.reflect.KClass

/**
 * Shared .NET defaults declared under `services { dotnet { ... } }`.
 */
data class DotnetDefaultsExtension(
    val target: DotnetTarget? = null,
    val solutions: Map<String, DotnetSolution> = emptyMap(),
    val model: DotnetDefaultsModel = DotnetDefaultsModel.empty(),
) : MicrosmithExtension, MergeableExtension<DotnetDefaultsExtension> {
    fun findSolution(name: String) = solutions[name]

    fun requireSolution(name: String): DotnetSolution {
        val normalized = validateDotnetQualifiedIdentifier(name, "Solution name")
        return findSolution(normalized) ?: error("Dotnet solution not found: $normalized")
    }

    fun allSolutions() = solutions.values

    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> get(type: KClass<T>) = model.get(type) as? T?

    inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    internal fun <T : MicrosmithExtension> with(type: KClass<T>, ext: T) = copy(
        model = model.with(type, ext),
    )

    override fun merge(other: DotnetDefaultsExtension): DotnetDefaultsExtension {
        val mergedSolutions = linkedMapOf<String, DotnetSolution>()

        solutions.values.forEach { solution ->
            mergedSolutions[solution.name] = solution
        }

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
