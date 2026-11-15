package io.github.lmliam.microsmith.dsl.services.dotnet.service

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.dsl.services.ServiceExtension
import kotlin.reflect.KClass

@InternalMicrosmithApi
public interface DotnetServiceContext : DotnetServiceScope {
    public fun <T : ServiceExtension> put(type: KClass<T>, ext: T)
}
