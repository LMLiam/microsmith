package io.github.lmliam.microsmith.dsl.services.dotnet.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.shared.DotnetSharedExtension

/**
 * Returns true if the shared dotnet model contains an extension of type [T].
 */
inline fun <reified T : MicrosmithExtension> DotnetSharedExtension.has() = get<T>() != null

/**
 * Returns the extension of type [T], or throws if not present.
 */
inline fun <reified T : MicrosmithExtension> DotnetSharedExtension.require() =
    get<T>() ?: error("Required dotnet shared extension ${T::class.simpleName} not found")

/**
 * Returns all extensions currently attached to the shared dotnet model.
 */
fun DotnetSharedExtension.extensions() = this.model.keys().mapNotNull { model.get(it) }

/**
 * Returns the set of extension types present in the shared dotnet model.
 */
fun DotnetSharedExtension.extensionTypes() = this.model.keys().map { it.java }.toSet()
