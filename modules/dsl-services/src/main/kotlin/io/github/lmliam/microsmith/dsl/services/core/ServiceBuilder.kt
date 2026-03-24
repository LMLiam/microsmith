package io.github.lmliam.microsmith.dsl.services.core

import kotlin.reflect.KClass

/**
 * Internal builder used to construct a [Service] from DSL blocks.
 */
class ServiceBuilder(
    private val name: String,
) : ServiceScope {
    private var model = ServiceModel.empty()

    fun <T : ServiceExtension> put(type: KClass<T>, ext: T) {
        model = model.with(type, ext)
    }

    fun build() = Service(name = name, model = model)
}
