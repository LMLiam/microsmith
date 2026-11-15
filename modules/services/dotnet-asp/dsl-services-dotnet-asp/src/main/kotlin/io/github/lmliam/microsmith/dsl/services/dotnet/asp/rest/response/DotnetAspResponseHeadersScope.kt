package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetAspResponseHeadersScope {
    public fun header(name: String): DotnetAspResponseHeader
}
