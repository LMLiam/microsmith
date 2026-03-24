package io.github.lmliam.microsmith.dsl.services.dotnet.core.solution

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import kotlin.reflect.KClass

interface DotnetSolutionContext : DotnetSolutionScope {
    fun <T : MicrosmithExtension> put(type: KClass<T>, ext: T)
}
