package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetSharedScope {
    fun target(target: DotnetTarget)

    fun solutions(block: DotnetSolutionsScope.() -> Unit)
}
