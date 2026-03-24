package io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import kotlin.reflect.KClass

interface DotnetDefaultsContext : DotnetDefaultsScope {
    fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T)
}
