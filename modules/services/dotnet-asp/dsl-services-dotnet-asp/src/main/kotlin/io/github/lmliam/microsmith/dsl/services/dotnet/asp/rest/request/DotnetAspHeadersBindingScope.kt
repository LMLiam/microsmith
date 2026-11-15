package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetAspHeadersBindingScope {
    public fun header(name: String): DotnetAspHeaderField
}
