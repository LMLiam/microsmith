package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspRequestFieldScope {
    fun optional()

    fun default(value: Any)
}
