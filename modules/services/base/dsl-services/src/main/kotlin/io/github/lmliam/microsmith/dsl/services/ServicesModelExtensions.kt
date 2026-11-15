package io.github.lmliam.microsmith.dsl.services

import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.ServicesModel

/** Returns true if the services-level shared model contains an extension of type [T]. */
internal inline fun <reified T : MicrosmithExtension> ServicesModel.has(): Boolean = get<T>() != null

/** Returns the shared services-level extension of type [T], or throws if not present. */
internal inline fun <reified T : MicrosmithExtension> ServicesModel.require(): T =
    get<T>() ?: error("Required services-level extension ${T::class.simpleName} not found")

/** Returns all extensions currently attached to the services-level shared model. */
internal fun ServicesModel.extensions(): List<MicrosmithExtension> = this.keys().mapNotNull { get(it) }

/** Returns the set of extension types present in the services-level shared model. */
internal fun ServicesModel.extensionTypes(): Set<Class<out MicrosmithExtension>> = this.keys().map { it.java }.toSet()
