package io.github.lmliam.microsmith.dsl.services

import kotlin.reflect.KClass

/** Internal builder used to construct a [Service] from DSL blocks. */
public class ServiceBuilder(private val name: String) : ServiceScope {
    private var model = ServiceModel.empty()

    public fun <T : ServiceExtension> put(type: KClass<T>, ext: T) {
        model = model.with(type, ext)
    }

    public fun build(): Service = Service(name = name, model = model)
}
