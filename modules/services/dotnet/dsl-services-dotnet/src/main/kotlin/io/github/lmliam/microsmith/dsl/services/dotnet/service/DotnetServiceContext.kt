package io.github.lmliam.microsmith.dsl.services.dotnet.service

import io.github.lmliam.microsmith.dsl.services.ServiceExtension
import kotlin.reflect.KClass

interface DotnetServiceContext : DotnetServiceScope {
    fun <T : ServiceExtension> put(type: KClass<T>, ext: T)
}
