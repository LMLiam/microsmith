package io.github.lmliam.microsmith.dsl

import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.MicrosmithModel

/** Returns true if the model contains an extension of type [T]. */
public inline fun <reified T : MicrosmithExtension> MicrosmithModel.has(): Boolean = get<T>() != null

/**
 * Returns the extension of type [T], or throws if not present.
 *
 * Useful when a plugin requires a specific extension to function.
 */
public inline fun <reified T : MicrosmithExtension> MicrosmithModel.require(): T =
    get<T>() ?: error("Required extension ${T::class.simpleName} not found")

/** Returns all extensions currently attached to the model. */
public fun MicrosmithModel.extensions(): List<MicrosmithExtension> = this.keys().mapNotNull { get(it) }

/** Returns the set of extension types present in the model. */
public fun MicrosmithModel.extensionTypes(): Set<Class<out MicrosmithExtension>> = this.keys().map { it.java }.toSet()
