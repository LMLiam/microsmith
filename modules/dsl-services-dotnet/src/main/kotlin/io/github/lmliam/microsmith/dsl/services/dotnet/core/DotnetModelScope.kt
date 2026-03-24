package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetModelScope {
    fun string(name: String): DotnetField

    fun int(name: String): DotnetField

    fun long(name: String): DotnetField

    fun bool(name: String): DotnetField

    infix fun String.ref(target: String): DotnetField
}
