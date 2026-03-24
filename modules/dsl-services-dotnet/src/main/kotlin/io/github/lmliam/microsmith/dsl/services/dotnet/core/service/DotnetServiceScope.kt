package io.github.lmliam.microsmith.dsl.services.dotnet.core.service

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.core.DotnetTarget
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelsScope

@MicrosmithDsl
interface DotnetServiceScope {
    fun target(target: DotnetTarget)

    fun solution(name: String)

    fun project(name: String)

    fun models(block: DotnetModelsScope.() -> Unit)
}
