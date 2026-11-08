package io.github.lmliam.microsmith.dsl.services.dotnet.defaults

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTargetAliases
import io.github.lmliam.microsmith.dsl.services.dotnet.solution.DotnetSolutionsScope

@MicrosmithDsl
interface DotnetDefaultsScope : DotnetTargetAliases {
    fun target(target: DotnetTarget)

    fun solutions(block: DotnetSolutionsScope.() -> Unit)
}
