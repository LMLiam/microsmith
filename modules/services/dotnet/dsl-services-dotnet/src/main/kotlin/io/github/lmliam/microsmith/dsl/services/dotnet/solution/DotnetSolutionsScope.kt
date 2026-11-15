package io.github.lmliam.microsmith.dsl.services.dotnet.solution

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetSolutionsScope {
    public operator fun String.invoke(block: DotnetSolutionScope.() -> Unit = {})

    public fun solution(name: String, block: DotnetSolutionScope.() -> Unit = {})
}
