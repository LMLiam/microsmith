package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspRequestBindingScope {
    fun string(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun char(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun byte(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun sbyte(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun short(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun ushort(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun int(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun uint(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun long(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun ulong(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun nint(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun nuint(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun float(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun double(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun decimal(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun bool(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun guid(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun dateOnly(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun timeOnly(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun dateTime(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun dateTimeOffset(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    fun timeSpan(name: String, block: DotnetAspRequestFieldScope.() -> Unit = {}): DotnetAspRequestField

    infix fun String.ref(target: String): DotnetAspRequestField

    infix fun String.references(target: String): DotnetAspRequestField
}
