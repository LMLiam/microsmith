package me.liam.microsmith.dsl.helpers

import me.liam.microsmith.dsl.core.MicrosmithExtension
import me.liam.microsmith.dsl.core.MicrosmithModel

/**
 * Returns true if the model contains an extension of type [T].
 */
inline fun <reified T : MicrosmithExtension> MicrosmithModel.has() = get<T>() != null

/**
 * Returns the extension of type [T], or throws if not present.
 *
 * Useful when a plugin requires a specific extension to function.
 */
inline fun <reified T : MicrosmithExtension> MicrosmithModel.require() =
    get<T>() ?: error("Required extension ${T::class.simpleName} not found")

/**
 * Returns all extensions currently attached to the model.
 */
fun MicrosmithModel.extensions() = this.keys().mapNotNull { get(it) }

/**
 * Returns the set of extension types present in the model.
 */
fun MicrosmithModel.extensionTypes() = this.keys().map { it.java }.toSet()