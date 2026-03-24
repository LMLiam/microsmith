package io.github.lmliam.microsmith.dsl.services.dotnet.core.solution

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetQualifiedIdentifier
import kotlin.reflect.KClass

/**
 * A named .NET solution declared in the shared dotnet scope.
 */
data class DotnetSolution(
    val name: String,
    val model: DotnetSolutionModel = DotnetSolutionModel.empty(),
) {
    init {
        validateDotnetQualifiedIdentifier(name, "Solution name")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> get(type: KClass<T>) = model.get(type) as? T?

    inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) = copy(
        model = model.with(type, value),
    )

    internal fun merge(other: DotnetSolution): DotnetSolution {
        require(name == other.name) {
            "Cannot merge .NET solutions with different names: '$name' and '${other.name}'."
        }

        return copy(model = model.merge(other.model))
    }
}
