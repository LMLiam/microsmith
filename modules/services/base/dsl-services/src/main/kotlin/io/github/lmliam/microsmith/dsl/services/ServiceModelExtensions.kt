package io.github.lmliam.microsmith.dsl.services

import io.github.lmliam.microsmith.dsl.services.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.ServiceModel

/** Returns true if the model contains an extension of type [T]. */
internal inline fun <reified T : ServiceExtension> ServiceModel.has(): Boolean = get<T>() != null

/** Returns the extension of type [T], or throws if not present. */
internal inline fun <reified T : ServiceExtension> ServiceModel.require(): T =
    get<T>() ?: error("Required service extension ${T::class.simpleName} not found")

/** Returns all extensions currently attached to the service model. */
internal fun ServiceModel.extensions(): List<ServiceExtension> = this.keys().mapNotNull { get(it) }

/** Returns the set of extension types present in the service model. */
internal fun ServiceModel.extensionTypes(): Set<Class<out ServiceExtension>> = this.keys().map { it.java }.toSet()
