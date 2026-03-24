package io.github.lmliam.microsmith.dsl.services.dotnet.core.shared

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import kotlin.reflect.KClass

interface DotnetSharedContext : DotnetSharedScope {
    fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T)
}
