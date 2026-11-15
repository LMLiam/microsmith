package io.github.lmliam.microsmith.dsl.services.dotnet.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTargetAliases
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModelsScope

@MicrosmithDsl
public interface DotnetServiceScope : DotnetTargetAliases {
    public fun target(target: DotnetTarget)

    public fun solution(name: String)

    public fun project(name: String)

    public fun models(block: DotnetModelsScope.() -> Unit)
}
