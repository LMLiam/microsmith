package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface DotnetModelsScope {
    operator fun String.invoke(block: DotnetModelScope.() -> Unit = {})

    fun model(name: String, block: DotnetModelScope.() -> Unit = {})
}
