package io.github.lmliam.microsmith.dsl.services.helpers

import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.core.ServiceModel

/**
 * Returns true if the model contains an extension of type [T].
 */
inline fun <reified T : ServiceExtension> ServiceModel.has() = get<T>() != null

/**
 * Returns the extension of type [T], or throws if not present.
 */
inline fun <reified T : ServiceExtension> ServiceModel.require() =
    get<T>() ?: error("Required service extension ${T::class.simpleName} not found")

/**
 * Returns all extensions currently attached to the service model.
 */
fun ServiceModel.extensions() = this.keys().mapNotNull { get(it) }

/**
 * Returns the set of extension types present in the service model.
 */
fun ServiceModel.extensionTypes() = this.keys().map { it.java }.toSet()
