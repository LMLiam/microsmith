package io.github.lmliam.microsmith.dsl.services.dotnet.core.solution

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetQualifiedIdentifier
import kotlin.reflect.KClass

internal class DotnetSolutionBuilder(
    private val name: String,
) : DotnetSolutionContext {
    private var model = DotnetSolutionModel.empty()

    override fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T) {
        model = model.with(type, ext)
    }

    fun build() = DotnetSolution(validateDotnetQualifiedIdentifier(name, "Solution name"), model)
}
