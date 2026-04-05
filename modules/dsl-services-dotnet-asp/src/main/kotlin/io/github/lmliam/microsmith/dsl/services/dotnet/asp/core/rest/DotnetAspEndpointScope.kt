package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelScope

@MicrosmithDsl
interface DotnetAspEndpointScope {
    fun path(name: String, block: DotnetAspRequestBindingScope.() -> Unit)

    fun query(name: String, block: DotnetAspRequestBindingScope.() -> Unit)

    fun headers(name: String, block: DotnetAspHeadersBindingScope.() -> Unit)

    fun body(modelName: String)

    fun body(name: String, block: DotnetModelScope.() -> Unit)

    fun responses(block: DotnetAspResponsesScope.() -> Unit)
}
