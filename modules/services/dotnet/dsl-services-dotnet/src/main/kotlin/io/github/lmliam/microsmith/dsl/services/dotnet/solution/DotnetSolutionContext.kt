package io.github.lmliam.microsmith.dsl.services.dotnet.solution

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import kotlin.reflect.KClass

@InternalMicrosmithApi
public interface DotnetSolutionContext : DotnetSolutionScope {
    public fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T)
}
