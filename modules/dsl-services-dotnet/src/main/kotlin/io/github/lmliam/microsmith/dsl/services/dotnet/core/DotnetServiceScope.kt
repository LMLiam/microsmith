package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetServiceScope {
    fun target(target: DotnetTarget)

    fun solution(name: String)

    fun project(name: String)

    fun models(block: DotnetModelsScope.() -> Unit)
}
