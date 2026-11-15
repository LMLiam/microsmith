package io.github.lmliam.microsmith.dsl.services.dotnet.asp.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetAspPortsScope {
    public fun http(port: Int)

    public fun https(port: Int)
}
