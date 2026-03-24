package io.github.lmliam.microsmith.dsl.services.dotnet.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsExtension

/**
 * Returns true if the dotnet defaults model contains an extension of type [T].
 */
inline fun <reified T : MicrosmithExtension> DotnetDefaultsExtension.has() = get<T>() != null

/**
 * Returns the extension of type [T], or throws if not present.
 */
inline fun <reified T : MicrosmithExtension> DotnetDefaultsExtension.require() =
    get<T>() ?: error("Required dotnet defaults extension ${T::class.simpleName} not found")

/**
 * Returns all extensions currently attached to the dotnet defaults model.
 */
fun DotnetDefaultsExtension.extensions() = this.model.keys().mapNotNull { model.get(it) }

/**
 * Returns the set of extension types present in the dotnet defaults model.
 */
fun DotnetDefaultsExtension.extensionTypes() = this.model.keys().map { it.java }.toSet()
