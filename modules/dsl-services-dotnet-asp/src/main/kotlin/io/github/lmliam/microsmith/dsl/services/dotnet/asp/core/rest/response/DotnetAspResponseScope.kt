package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelScope

@MicrosmithDsl
interface DotnetAspResponseScope {
    fun model(block: DotnetModelScope.() -> Unit)

    fun headers(block: DotnetAspResponseHeadersScope.() -> Unit)
}
