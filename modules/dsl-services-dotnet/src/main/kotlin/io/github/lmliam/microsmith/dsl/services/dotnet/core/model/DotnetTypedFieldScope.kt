package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetTypedFieldScope<TField> {
    fun string(name: String): TField

    fun char(name: String): TField

    fun byte(name: String): TField

    fun sbyte(name: String): TField

    fun short(name: String): TField

    fun ushort(name: String): TField

    fun int(name: String): TField

    fun uint(name: String): TField

    fun long(name: String): TField

    fun ulong(name: String): TField

    fun nint(name: String): TField

    fun nuint(name: String): TField

    fun float(name: String): TField

    fun double(name: String): TField

    fun decimal(name: String): TField

    fun bool(name: String): TField

    fun guid(name: String): TField

    fun dateOnly(name: String): TField

    fun timeOnly(name: String): TField

    fun dateTime(name: String): TField

    fun dateTimeOffset(name: String): TField

    fun timeSpan(name: String): TField

    infix fun String.ref(target: String): TField

    infix fun String.references(target: String): TField
}
