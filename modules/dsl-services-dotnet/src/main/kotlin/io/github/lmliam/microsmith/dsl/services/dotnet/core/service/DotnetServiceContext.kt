package io.github.lmliam.microsmith.dsl.services.dotnet.core.service

import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import kotlin.reflect.KClass

interface DotnetServiceContext : DotnetServiceScope {
    fun <T : ServiceExtension> put(type: KClass<T>, ext: T)
}
