package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetModelScope {
    fun string(name: String): DotnetField

    fun char(name: String): DotnetField

    fun byte(name: String): DotnetField

    fun short(name: String): DotnetField

    fun int(name: String): DotnetField

    fun long(name: String): DotnetField

    fun float(name: String): DotnetField

    fun double(name: String): DotnetField

    fun decimal(name: String): DotnetField

    fun bool(name: String): DotnetField

    fun guid(name: String): DotnetField

    fun dateOnly(name: String): DotnetField

    fun timeOnly(name: String): DotnetField

    fun dateTime(name: String): DotnetField

    fun dateTimeOffset(name: String): DotnetField

    fun timeSpan(name: String): DotnetField

    infix fun String.ref(target: String): DotnetField
}
