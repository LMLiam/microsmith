package io.github.lmliam.microsmith.dsl.services.dotnet.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetSolutionModel

/**
 * Returns true if the solution model contains an extension of type [T].
 */
inline fun <reified T : MicrosmithExtension> DotnetSolutionModel.has() = get<T>() != null

/**
 * Returns the extension of type [T], or throws if not present.
 */
inline fun <reified T : MicrosmithExtension> DotnetSolutionModel.require() =
    get<T>() ?: error("Required dotnet solution extension ${T::class.simpleName} not found")

/**
 * Returns all extensions currently attached to the solution model.
 */
fun DotnetSolutionModel.extensions() = this.keys().mapNotNull { get(it) }

/**
 * Returns the set of extension types present in the solution model.
 */
fun DotnetSolutionModel.extensionTypes() = this.keys().map { it.java }.toSet()
