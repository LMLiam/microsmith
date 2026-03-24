package io.github.lmliam.microsmith.dsl.services.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.core.ServicesModel

/**
 * Returns true if the services-level shared model contains an extension of type [T].
 */
inline fun <reified T : MicrosmithExtension> ServicesModel.has() = get<T>() != null

/**
 * Returns the shared services-level extension of type [T], or throws if not present.
 */
inline fun <reified T : MicrosmithExtension> ServicesModel.require() =
    get<T>() ?: error("Required services-level extension ${T::class.simpleName} not found")

/**
 * Returns all extensions currently attached to the services-level shared model.
 */
fun ServicesModel.extensions() = this.keys().mapNotNull { get(it) }

/**
 * Returns the set of extension types present in the services-level shared model.
 */
fun ServicesModel.extensionTypes() = this.keys().map { it.java }.toSet()
