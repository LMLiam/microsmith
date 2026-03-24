package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetSolutionsScope {
    operator fun String.invoke(block: DotnetSolutionScope.() -> Unit = {})
}
