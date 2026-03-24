package io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.DotnetSolutionsScope

@MicrosmithDsl
interface DotnetDefaultsScope {
    fun target(target: DotnetTarget)

    fun solutions(block: DotnetSolutionsScope.() -> Unit)
}
