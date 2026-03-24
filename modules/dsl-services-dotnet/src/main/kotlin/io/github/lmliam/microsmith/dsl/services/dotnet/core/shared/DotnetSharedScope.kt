package io.github.lmliam.microsmith.dsl.services.dotnet.core.shared

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolutionsScope

@MicrosmithDsl
interface DotnetSharedScope {
    fun target(target: DotnetTarget)

    fun solutions(block: DotnetSolutionsScope.() -> Unit)
}
