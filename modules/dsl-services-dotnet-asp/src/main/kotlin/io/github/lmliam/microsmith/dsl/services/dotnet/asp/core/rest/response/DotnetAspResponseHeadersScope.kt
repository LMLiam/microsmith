package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspResponseHeadersScope {
    fun header(name: String): DotnetAspResponseHeader
}
