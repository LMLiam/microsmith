package io.github.lmliam.microsmith.dsl.services.dotnet.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.DotnetTargetAliases
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModelsScope

@MicrosmithDsl
interface DotnetServiceScope : DotnetTargetAliases {
    fun target(target: DotnetTarget)

    fun solution(name: String)

    fun project(name: String)

    fun models(block: DotnetModelsScope.() -> Unit)
}
