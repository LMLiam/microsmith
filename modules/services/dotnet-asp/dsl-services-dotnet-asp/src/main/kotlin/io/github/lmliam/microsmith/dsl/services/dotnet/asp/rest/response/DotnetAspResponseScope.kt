package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModelScope

@MicrosmithDsl
public interface DotnetAspResponseScope {
    public fun model(block: DotnetModelScope.() -> Unit)

    public fun headers(block: DotnetAspResponseHeadersScope.() -> Unit)
}
