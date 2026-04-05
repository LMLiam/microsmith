package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspRequestFieldScope {
    fun optional()

    fun default(value: Any)
}
