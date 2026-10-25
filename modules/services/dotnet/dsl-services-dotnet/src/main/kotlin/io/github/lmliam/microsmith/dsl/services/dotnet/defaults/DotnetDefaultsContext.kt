package io.github.lmliam.microsmith.dsl.services.dotnet.defaults

import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import kotlin.reflect.KClass

interface DotnetDefaultsContext : DotnetDefaultsScope {
    fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T)
}
