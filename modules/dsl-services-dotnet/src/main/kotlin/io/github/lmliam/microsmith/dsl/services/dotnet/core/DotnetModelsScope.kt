package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetModelsScope {
    operator fun String.invoke(block: DotnetModelScope.() -> Unit = {})
}
