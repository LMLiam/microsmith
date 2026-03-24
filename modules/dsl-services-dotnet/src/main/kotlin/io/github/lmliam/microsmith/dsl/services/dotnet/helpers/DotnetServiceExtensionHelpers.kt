package io.github.lmliam.microsmith.dsl.services.dotnet.helpers

import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension

/**
 * Returns true if the service model contains an extension of type [T].
 */
inline fun <reified T : ServiceExtension> DotnetServiceExtension.has() = get<T>() != null

/**
 * Returns the extension of type [T], or throws if not present.
 */
inline fun <reified T : ServiceExtension> DotnetServiceExtension.require() =
    get<T>() ?: error("Required dotnet service extension ${T::class.simpleName} not found")

/**
 * Returns all extensions currently attached to the service dotnet model.
 */
fun DotnetServiceExtension.extensions() = this.model.keys().mapNotNull { model.get(it) }

/**
 * Returns the set of extension types present in the service dotnet model.
 */
fun DotnetServiceExtension.extensionTypes() = this.model.keys().map { it.java }.toSet()
