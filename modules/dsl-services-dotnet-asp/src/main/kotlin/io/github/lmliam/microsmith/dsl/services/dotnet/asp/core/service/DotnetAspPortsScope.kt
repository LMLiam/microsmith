package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspPortsScope {
    fun http(port: Int)

    fun https(port: Int)
}
