package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspHeadersBindingScope {
    fun header(name: String): DotnetAspHeaderField
}
