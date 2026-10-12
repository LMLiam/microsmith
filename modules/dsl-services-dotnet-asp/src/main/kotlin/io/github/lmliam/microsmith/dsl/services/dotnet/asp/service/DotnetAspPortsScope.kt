package io.github.lmliam.microsmith.dsl.services.dotnet.asp.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspPortsScope {
    fun http(port: Int)

    fun https(port: Int)
}
