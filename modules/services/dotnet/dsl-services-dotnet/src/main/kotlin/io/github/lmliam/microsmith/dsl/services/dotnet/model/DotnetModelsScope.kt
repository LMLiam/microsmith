package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetModelsScope {
    public operator fun String.invoke(block: DotnetModelScope.() -> Unit = {})

    public fun model(name: String, block: DotnetModelScope.() -> Unit = {})
}
