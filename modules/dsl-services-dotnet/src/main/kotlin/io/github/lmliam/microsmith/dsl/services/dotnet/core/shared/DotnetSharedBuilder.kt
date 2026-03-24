package io.github.lmliam.microsmith.dsl.services.dotnet.core.shared

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolution
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolutionsBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolutionsScope
import kotlin.reflect.KClass

internal class DotnetSharedBuilder : DotnetSharedContext {
    private var target: DotnetTarget? = null
    private val solutionsByName = linkedMapOf<String, DotnetSolution>()
    private var model = DotnetSharedModel.empty()

    override fun target(target: DotnetTarget) {
        this.target = target
    }

    override fun solutions(block: DotnetSolutionsScope.() -> Unit) {
        val builder = DotnetSolutionsBuilder().apply(block)
        builder.build().values.forEach { solution ->
            require(solution.name !in solutionsByName) {
                "Duplicate .NET solution registration for '${solution.name}'."
            }
            solutionsByName[solution.name] = solution
        }
    }

    override fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T) {
        model = model.with(type, ext)
    }

    fun build() = DotnetSharedExtension(
        target = target,
        solutions = solutionsByName.toMap(),
        model = model,
    )
}
