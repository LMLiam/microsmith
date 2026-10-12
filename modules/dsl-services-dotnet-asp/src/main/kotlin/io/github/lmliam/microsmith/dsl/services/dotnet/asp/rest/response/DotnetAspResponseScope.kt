package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModelScope

@MicrosmithDsl
interface DotnetAspResponseScope {
    fun model(block: DotnetModelScope.() -> Unit)

    fun headers(block: DotnetAspResponseHeadersScope.() -> Unit)
}
