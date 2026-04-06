package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetConfigurableTypedFieldScope<TField, TOptionsScope> {
    fun string(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun char(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun byte(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun sbyte(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun short(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun ushort(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun int(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun uint(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun long(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun ulong(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun nint(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun nuint(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun float(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun double(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun decimal(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun bool(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun guid(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun dateOnly(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun timeOnly(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun dateTime(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun dateTimeOffset(name: String, block: TOptionsScope.() -> Unit = {}): TField

    fun timeSpan(name: String, block: TOptionsScope.() -> Unit = {}): TField

    infix fun String.ref(target: String): TField

    infix fun String.references(target: String): TField
}
