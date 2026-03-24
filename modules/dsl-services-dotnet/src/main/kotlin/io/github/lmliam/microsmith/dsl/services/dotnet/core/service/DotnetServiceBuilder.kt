package io.github.lmliam.microsmith.dsl.services.dotnet.core.service

import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelsBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelsScope
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetQualifiedIdentifier
import kotlin.reflect.KClass

internal class DotnetServiceBuilder : DotnetServiceContext {
    private var target: DotnetTarget? = null
    private var solution: String? = null
    private var project: String? = null
    private val modelsByName = linkedMapOf<String, DotnetModel>()
    private var model = DotnetServiceModel.empty()

    override fun target(target: DotnetTarget) {
        this.target = target
    }

    override fun solution(name: String) {
        solution = validateDotnetQualifiedIdentifier(name, "Solution name")
    }

    override fun project(name: String) {
        project = validateDotnetQualifiedIdentifier(name, "Project name")
    }

    override fun models(block: DotnetModelsScope.() -> Unit) {
        val builder = DotnetModelsBuilder().apply(block)
        builder.build().forEach { (name, model) ->
            require(name !in modelsByName) {
                "Duplicate .NET model registration for '$name'."
            }
            modelsByName[name] = model
        }
    }

    override fun <T : ServiceExtension> put(type: KClass<T>, ext: T) {
        model = model.with(type, ext)
    }

    fun build() = DotnetServiceExtension(
        target = target,
        solution = solution,
        project = project,
        models = modelsByName.toMap(),
        model = model,
    )
}
